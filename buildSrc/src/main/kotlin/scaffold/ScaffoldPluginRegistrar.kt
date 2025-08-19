package scaffold

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.nio.file.Files
import java.nio.file.Path
import java.util.regex.Pattern

object ScaffoldPluginRegistrar {

    private val mapper: ObjectMapper = jacksonObjectMapper()

    // ---------- Public API ----------

    fun registerAll(
        artifactName: String,
        classPrefix: String,
        functionPrefix: String
    ) {
        val frontendRoot: Path = Path.of("frontend")
        val provideToken = "PLUGINS_TOKEN"

        registerTsconfigPath(frontendRoot.resolve("tsconfig.json"), artifactName)
        registerPackageJson(frontendRoot.resolve("package.json"), artifactName)
        registerAngularProject(frontendRoot.resolve("angular.json"), artifactName)
        registerAppModule(
            moduleTs = frontendRoot.resolve("src/app/app.module.ts"),
            artifactName = artifactName,
            classPrefix = classPrefix,
            functionPrefix = functionPrefix,
            provideToken = provideToken
        )
    }

    // ---------- tsconfig.json ----------

    fun registerTsconfigPath(tsconfigPath: Path, artifactName: String) {
        val root = readJsonObject(tsconfigPath)
        val paths = ensureObject(root, "/compilerOptions/paths")

        val key = "@valtimo-plugins/$artifactName"
        val desired = arrayOf("dist/valtimo-plugins/$artifactName")

        val existing = paths.get(key)
        if (existing is ArrayNode) {
            if (!arrayEquals(existing, desired)) {
                // Replace with the single desired path
                paths.set<ArrayNode>(key, mapper.createArrayNode().add(desired[0]))
            }
        } else {
            paths.set<ArrayNode>(key, mapper.createArrayNode().add(desired[0]))
        }

        writeJson(tsconfigPath, root)
    }

    // ---------- package.json (devDependencies + scripts) ----------

    fun registerPackageJson(packageJsonPath: Path, artifactName: String) {
        val root = readJsonObject(packageJsonPath)

        // devDependencies["@valtimo-plugins/<artifact>"] = "file:dist/valtimo-plugins/<artifact>"
        val devDeps = ensureObject(root, "/devDependencies")
        val pkgKey = "@valtimo-plugins/$artifactName"
        val desiredVersion = "file:dist/valtimo-plugins/$artifactName"
        if (devDeps.get(pkgKey)?.asText() != desiredVersion) {
            devDeps.put(pkgKey, desiredVersion)
        }

        // scripts:
        // "libs:build:<artifact>" = "ng build @valtimo-plugins/<artifact>"
        // "libs:watch:<artifact>" = "ng build @valtimo-plugins/<artifact> --watch"
        val scripts = ensureObject(root, "/scripts")
        val buildKey = "libs:build:$artifactName"
        val watchKey = "libs:watch:$artifactName"
        val buildCmd = "ng build @valtimo-plugins/$artifactName"
        val watchCmd = "$buildCmd --watch"

        if (scripts.get(buildKey)?.asText() != buildCmd) scripts.put(buildKey, buildCmd)
        if (scripts.get(watchKey)?.asText() != watchCmd) scripts.put(watchKey, watchCmd)

        writeJson(packageJsonPath, root)
    }

    // ---------- angular.json (projects entry) ----------

    fun registerAngularProject(angularJsonPath: Path, artifactName: String) {
        val root = readJsonObject(angularJsonPath)
        val projects = ensureObject(root, "/projects")

        val projectKey = "@valtimo-plugins/$artifactName"
        val desired = buildAngularProjectNode(artifactName)

        // If project exists, shallow replace to keep it up-to-date
        val existing = projects.get(projectKey)
        if (existing !is ObjectNode || !objectShallowEquals(existing, desired)) {
            projects.set<ObjectNode>(projectKey, desired)
        }

        writeJson(angularJsonPath, root)
    }

    // ---------- app.module.ts (imports + providers) ----------

    fun registerAppModule(
        moduleTs: Path,
        artifactName: String,
        classPrefix: String,
        functionPrefix: String,
        provideToken: String
    ) {
        var code = Files.readString(moduleTs)

        // 1) Ensure imports
        code = ensureNamedImport(
            code,
            symbol = "${classPrefix}PluginModule",
            fromPath = "../../projects/valtimo-plugins/$artifactName/src/lib/${artifactName}-plugin.module"
        )

        code = ensureNamedImport(
            code,
            symbol = "${functionPrefix}PluginSpecification",
            fromPath = "../../projects/valtimo-plugins/$artifactName/src/lib/${artifactName}.plugin.specification"
        )

        // 2) Add module to @NgModule({ imports: [...] })
        code = addSymbolToNgModuleArray(code, arrayName = "imports", symbol = "${classPrefix}PluginModule")

        // 3) Add provider with useValue to @NgModule({ providers: [...] })
        //    Object inserted: { provide: <provideToken>, useValue: <functionPrefix>PluginSpecification, multi: true }
        code = addProviderUseValue(
            code = code,
            provideToken = provideToken,
            useValueSymbol = "${functionPrefix}PluginSpecification",
            multi = true
        )

        Files.writeString(moduleTs, code)
    }

    // ---------- Helpers: JSON ----------

    private fun readJsonObject(path: Path): ObjectNode {
        val bytes = Files.readAllBytes(path)
        val node = mapper.readTree(bytes)
        require(node is ObjectNode) { "Root of ${path.fileName} must be a JSON object" }
        return node
    }

    private fun writeJson(path: Path, root: JsonNode) {
        mapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), root)
    }

    private fun ensureObject(root: ObjectNode, pointerStr: String): ObjectNode {
        if (pointerStr == "" || pointerStr == "/") return root
        val tokens = pointerStr.split("/").filter { it.isNotBlank() }
        var cursor: ObjectNode = root
        for (token in tokens) {
            val child = cursor.get(token)
            cursor = if (child == null || child.isNull) {
                val created = mapper.createObjectNode()
                cursor.set<ObjectNode>(token, created)
                created
            } else {
                require(child is ObjectNode) { "Expected object at '$token' but found ${child.nodeType}" }
                child
            }
        }
        return cursor
    }

    private fun arrayEquals(arr: ArrayNode, desired: Array<String>): Boolean {
        if (arr.size() != desired.size) return false
        for (i in 0 until arr.size()) {
            if (arr.get(i).asText() != desired[i]) return false
        }
        return true
    }

    private fun objectShallowEquals(a: ObjectNode, b: ObjectNode): Boolean {
        val aFields = a.fieldNames().asSequence().toSet()
        val bFields = b.fieldNames().asSequence().toSet()
        if (aFields != bFields) return false
        for (name in aFields) {
            if (a.get(name) != b.get(name)) return false
        }
        return true
    }

    private fun buildAngularProjectNode(artifactName: String): ObjectNode {
        val o = mapper.createObjectNode()
        o.put("projectType", "library")
        o.put("root", "projects/valtimo-plugins/$artifactName")
        o.put("sourceRoot", "projects/valtimo-plugins/$artifactName/src")
        o.put("prefix", "lib")

        val architect = mapper.createObjectNode()
        val build = mapper.createObjectNode().apply {
            put("builder", "@angular-devkit/build-angular:ng-packagr")
            set<ObjectNode>("options", mapper.createObjectNode().apply {
                put("tsConfig", "projects/valtimo-plugins/$artifactName/tsconfig.lib.json")
                put("project", "projects/valtimo-plugins/$artifactName/ng-package.json")
            })
            set<ObjectNode>("configurations", mapper.createObjectNode().apply {
                set<ObjectNode>("production", mapper.createObjectNode().apply {
                    put("tsConfig", "projects/valtimo-plugins/$artifactName/tsconfig.lib.prod.json")
                })
                set<ObjectNode>("development", mapper.createObjectNode().apply {
                    // Keep development pointing at the artifact (not alfresco-auth)
                    put("tsConfig", "projects/valtimo-plugins/$artifactName/tsconfig.lib.json")
                })
            })
            put("defaultConfiguration", "production")
        }

        val test = mapper.createObjectNode().apply {
            put("builder", "@angular-devkit/build-angular:karma")
            set<ObjectNode>("options", mapper.createObjectNode().apply {
                put("main", "projects/valtimo-plugins/$artifactName/src/test.ts")
                put("tsConfig", "projects/valtimo-plugins/$artifactName/tsconfig.spec.json")
                put("karmaConfig", "projects/valtimo-plugins/$artifactName/karma.conf.js")
            })
        }

        val lint = mapper.createObjectNode().apply {
            put("builder", "@angular-eslint/builder:lint")
            set<ObjectNode>("options", mapper.createObjectNode().apply {
                set<ArrayNode>("lintFilePatterns", mapper.createArrayNode().apply {
                    add("projects/valtimo-plugins/$artifactName/**/*.ts")
                    add("projects/valtimo-plugins/$artifactName/**/*.html")
                })
            })
        }

        architect.set<ObjectNode>("build", build)
        architect.set<ObjectNode>("test", test)
        architect.set<ObjectNode>("lint", lint)
        o.set<ObjectNode>("architect", architect)

        return o
    }

    // ---------- Helpers: TypeScript editing ----------

    private fun ensureNamedImport(code: String, symbol: String, fromPath: String): String {
        val importRegex = Regex(
            """^import\s*\{[^}]*\b${Pattern.quote(symbol)}\b[^}]*}\s*from\s*['"]${Pattern.quote(fromPath)}['"]\s*;""",
            RegexOption.MULTILINE
        )
        if (importRegex.containsMatchIn(code)) return code

        val importFromSamePath = Regex(
            """^import\s*\{([^}]*)}\s*from\s*['"]${Pattern.quote(fromPath)}['"]\s*;""",
            RegexOption.MULTILINE
        )

        val m = importFromSamePath.find(code)
        return if (m != null) {
            val existingInside = m.groupValues[1]
            val merged = mergeSymbolList(existingInside, symbol)
            code.replaceRange(m.range, "import { $merged } from '$fromPath';")
        } else {
            val lastImport = Regex("""^import .*;[ \t]*\r?\n""", RegexOption.MULTILINE).findAll(code).lastOrNull()
            val ins = "import { $symbol } from '$fromPath';\n"
            if (lastImport != null) {
                val p = lastImport.range.last + 1
                code.substring(0, p) + ins + code.substring(p)
            } else {
                "$ins$code"
            }
        }
    }

    private fun mergeSymbolList(existing: String, symbol: String): String {
        val parts = existing.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableSet()
        parts.add(symbol)
        return parts.joinToString(", ")
    }

    private fun addSymbolToNgModuleArray(code: String, arrayName: String, symbol: String): String {
        val range = findNgModuleRange(code) ?: return code
        val ng = code.substring(range.first, range.last + 1)

        val arrayRegex = Regex("""($arrayName\s*:\s*\[)([\s\S]*?)(])""")
        val m = arrayRegex.find(ng)
        val updated = if (m != null) {
            val body = m.groupValues[2]
            val tokenRegex = Regex("""\b${Pattern.quote(symbol)}\b""")
            if (tokenRegex.containsMatchIn(body)) ng
            else {
                val newBody = if (body.trim().isEmpty()) " $symbol " else {
                    if (body.trim().endsWith(",")) "$body $symbol," else "$body, $symbol"
                }
                ng.replaceRange(m.range, m.groupValues[1] + newBody + m.groupValues[3])
            }
        } else {
            // create the property
            val insertAt = findInsertionBeforeClosingBrace(ng)
            ng.substring(0, insertAt) + "  $arrayName: [ $symbol ],\n" + ng.substring(insertAt)
        }

        return code.replaceRange(range, updated)
    }

    private fun addProviderUseValue(
        code: String,
        provideToken: String,
        useValueSymbol: String,
        multi: Boolean = true
    ): String {
        val range = findNgModuleRange(code) ?: return code
        var ng = code.substring(range.first, range.last + 1)

        // Ensure imports contain the symbol; handled earlier but harmless to check
        // Find providers array:
        val arrayRegex = Regex("""(providers\s*:\s*\[)([\s\S]*?)(])""")
        val m = arrayRegex.find(ng)

        val providerObject = buildProviderObject(provideToken, useValueSymbol, multi)

        ng = if (m != null) {
            val body = m.groupValues[2]
            // Check if an object with same useValue already exists
            val exists = Regex("""useValue\s*:\s*\b${Pattern.quote(useValueSymbol)}\b""").containsMatchIn(body)
            if (exists) ng
            else {
                val newBody = if (body.trim().isEmpty()) " $providerObject " else {
                    if (body.trim().endsWith(",")) "$body $providerObject," else "$body, $providerObject"
                }
                ng.replaceRange(m.range, m.groupValues[1] + newBody + m.groupValues[3])
            }
        } else {
            val insertAt = findInsertionBeforeClosingBrace(ng)
            ng.substring(0, insertAt) + "  providers: [ $providerObject ],\n" + ng.substring(insertAt)
        }

        return code.replaceRange(range, ng)
    }

    private fun buildProviderObject(provideToken: String, useValueSymbol: String, multi: Boolean): String {
        return if (multi) "{ provide: $provideToken, useValue: $useValueSymbol, multi: true }"
        else "{ provide: $provideToken, useValue: $useValueSymbol }"
    }

    private fun findNgModuleRange(code: String): IntRange? {
        val start = Regex("""@NgModule\s*\(\s*\{""").find(code)?.range?.first ?: return null
        var i = code.indexOf('{', start)
        if (i < 0) return null
        var depth = 1
        i++
        while (i < code.length && depth > 0) {
            when (code[i]) {
                '{' -> depth++
                '}' -> depth--
            }
            i++
        }
        if (depth != 0) return null
        var j = i
        while (j < code.length && code[j].isWhitespace()) j++
        if (j < code.length && code[j] == ')') j++ else return null
        return start..(j - 1)
    }

    private fun findInsertionBeforeClosingBrace(ngModuleBlock: String): Int {
        var i = ngModuleBlock.lastIndexOf('}')
        if (i <= 0) error("Malformed @NgModule block")
        while (i > 0 && ngModuleBlock[i - 1].isWhitespace()) i--
        return i
    }
}