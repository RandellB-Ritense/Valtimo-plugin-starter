package scaffold

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.gradle.api.logging.Logger
import java.nio.file.Files
import java.nio.file.Path
import java.util.regex.Pattern
import kotlin.io.path.readText
import kotlin.io.path.writeText

object ScaffoldPluginRegistrarFrontend {

    private val mapper: ObjectMapper = jacksonObjectMapper()

    fun registerFrontend(frontendRoot: Path, artifactName: String, logger: Logger) {
        addScripts(frontendRoot, artifactName, logger)
        addDevDependency(frontendRoot, artifactName, logger)
        addTsconfigPath(frontendRoot, artifactName, logger)
        addAngularProject(frontendRoot, artifactName, logger)
    }

    fun addScripts(frontendRoot: Path, artifactName: String, logger: Logger) {
        require(Files.exists(frontendRoot.resolve("package.json"))) {
            "package.json not found in $frontendRoot"
        }

        val npm = if (isWindows()) "npm.cmd" else "npm"
        val buildCmd = "ng build @valtimo-plugins/$artifactName"
        val watchCmd = "$buildCmd --watch"

        runNpm(frontendRoot, logger, npm, "pkg", "set", "scripts[libs:build:$artifactName]=$buildCmd")
        runNpm(frontendRoot, logger, npm, "pkg", "set", "scripts[libs:watch:$artifactName]=$watchCmd")
    }

    fun addDevDependency(frontendRoot: Path, artifactName: String, logger: Logger) {
        require(Files.exists(frontendRoot.resolve("package.json"))) {
            "package.json not found in $frontendRoot"
        }

        val npm = if (isWindows()) "npm.cmd" else "npm"
        val depKey = "@valtimo-plugins/$artifactName"
        val depVal = "file:dist/valtimo-plugins/$artifactName"

        runNpm(frontendRoot, logger, npm, "pkg", "set", "devDependencies['$depKey']=$depVal")
    }

    /**
     * Adds a path alias in tsconfig.json:
     *
     * "@valtimo-plugins/<artifact>": ["dist/valtimo-plugins/<artifact>"]
     */
    fun addTsconfigPath(frontendRoot: Path, artifactName: String, logger: Logger) {
        val tsconfigPath = frontendRoot.resolve("tsconfig.json")
        require(Files.exists(tsconfigPath)) { "tsconfig.json not found in $frontendRoot" }

        val raw = tsconfigPath.readText()
        val root = mapper.readTree(sanitizeJson(raw)) as ObjectNode

        val compilerOptions = (root.get("compilerOptions") as? ObjectNode)
            ?: root.putObject("compilerOptions")

        val paths = (compilerOptions.get("paths") as? ObjectNode)
            ?: compilerOptions.putObject("paths")


        val key = "@valtimo-plugins/$artifactName"
        val desiredPath = "dist/valtimo-plugins/$artifactName"

        val current = paths.get(key)
        val shouldWrite = when (current) {
            null -> true
            is ArrayNode -> current.none { it.asText() == desiredPath }
            else -> true
        }

        if (shouldWrite) {
            val arr = (current as? ArrayNode) ?: mapper.createArrayNode()
            if (arr.none { it.asText() == desiredPath }) arr.add(desiredPath)
            paths.set<ArrayNode>(key, arr)
            tsconfigPath.writeText(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root))
            logger.lifecycle("Added tsconfig path: $key -> $desiredPath")
        } else {
            logger.info("tsconfig path already present: $key -> $desiredPath")
        }
    }

    // --- angular.json: add/update project entry ----------------------------------

    fun addAngularProject(frontendRoot: Path, artifactName: String, logger: Logger) {
        val angularJson = frontendRoot.resolve("angular.json")
        require(Files.exists(angularJson)) { "angular.json not found in $frontendRoot" }

        val root = mapper.readTree(Files.readString(angularJson)) as ObjectNode
        val projectsNode = root.get("projects")
        val projects = when {
            projectsNode == null || projectsNode.isNull -> root.with("projects")
            projectsNode.isObject -> projectsNode as ObjectNode
            else -> error("""angular.json: "projects" must be an object""")
        }
        val key = "@valtimo-plugins/$artifactName"

        projects.set<ObjectNode>(key, buildAngularProjectNode(artifactName))

        Files.writeString(
            angularJson,
            mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root)
        )
        logger.lifecycle("""angular.json → projects["$key"] updated.""")
    }

    private fun buildAngularProjectNode(artifact: String): ObjectNode {
        fun obj() = mapper.createObjectNode()

        val project = obj().apply {
            put("projectType", "library")
            put("root", "projects/valtimo-plugins/$artifact")
            put("sourceRoot", "projects/valtimo-plugins/$artifact/src")
            put("prefix", "lib")
        }

        val build = obj().apply {
            put("builder", "@angular-devkit/build-angular:ng-packagr")
            set<ObjectNode>("options", obj().apply {
                put("project", "projects/valtimo-plugins/$artifact/ng-package.json")
            })
            set<ObjectNode>("configurations", obj().apply {
                set<ObjectNode>("production", obj().apply {
                    put("tsConfig", "projects/valtimo-plugins/$artifact/tsconfig.lib.prod.json")
                })
                set<ObjectNode>("development", obj().apply {
                    put("tsConfig", "projects/valtimo-plugins/$artifact/tsconfig.lib.json")
                })
            })
            put("defaultConfiguration", "production")
        }

        val test = obj().apply {
            put("builder", "@angular-devkit/build-angular:karma")
            set<ObjectNode>("options", obj().apply {
                put("tsConfig", "projects/valtimo-plugins/$artifact/tsconfig.spec.json")
                set<ArrayNode>("polyfills", mapper.createArrayNode().apply {
                    add("zone.js")
                    add("zone.js/testing")
                })
            })
        }

        val architect = obj().apply {
            set<ObjectNode>("build", build)
            set<ObjectNode>("test", test)
        }

        project.set<ObjectNode>("architect", architect)
        return project
    }
    // --- helpers ---

    private fun runNpm(workingDir: Path, logger: Logger, vararg args: String) {
        val pb = ProcessBuilder(*args)
            .directory(workingDir.toFile())
            .redirectErrorStream(true)

        val proc = pb.start()
        val output = proc.inputStream.bufferedReader().use { it.readText() }
        val exit = proc.waitFor()

        if (exit != 0) {
            throw IllegalStateException("Command failed (${args.joinToString(" ")}):\n$output")
        } else {
            logger.lifecycle(output.trim().ifEmpty { args.joinToString(" ") })
        }
    }

    private fun sanitizeJson(raw: String): String {
        // Remove // line comments
        var s = raw.replace(Regex("(?m)//.*$"), "")
        // Remove /* block */ comments
        s = s.replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), "")
        // Remove trailing commas before } or ]
        s = s.replace(Regex(",\\s*([}\\]])"), "$1")
        return s
    }

    private fun isWindows(): Boolean =
        System.getProperty("os.name").lowercase().contains("win")
}