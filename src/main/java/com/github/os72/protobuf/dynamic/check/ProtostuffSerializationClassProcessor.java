package com.github.os72.protobuf.dynamic.check;

import com.alibaba.fastjson.JSON;
import com.google.auto.service.AutoService;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic.Kind;

/**
 * @author ironman
 * @date 2023/7/27 11:23
 * @desc
 */

@SupportedAnnotationTypes("com.github.os72.protobuf.dynamic.check.ProtostuffSerializationClass")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@AutoService(Processor.class)
public class ProtostuffSerializationClassProcessor extends AbstractProcessor {

    private static final Map<String, String> TYPE_MAPPING = new HashMap<>(32);

    static {
        TYPE_MAPPING.put("int", "int");
        TYPE_MAPPING.put("float", "float");
        TYPE_MAPPING.put("double", "double");
        TYPE_MAPPING.put("long", "long");
        TYPE_MAPPING.put("byte", "byte");
        TYPE_MAPPING.put("boolean", "boolean");
        TYPE_MAPPING.put("char", "char");

        TYPE_MAPPING.put("java.lang.Integer", "java.lang.Integer");
        TYPE_MAPPING.put("java.lang.Float", "java.lang.Float");
        TYPE_MAPPING.put("java.lang.Double", "java.lang.Double");
        TYPE_MAPPING.put("java.lang.Byte", "java.lang.Byte");
        TYPE_MAPPING.put("java.lang.Long", "java.lang.Long");
        TYPE_MAPPING.put("java.lang.Character", "java.lang.Character");
        TYPE_MAPPING.put("java.lang.String", "java.lang.String");
        TYPE_MAPPING.put("java.lang.Enum", "java.lang.Enum");
        TYPE_MAPPING.put("java.lang.Boolean", "java.lang.Boolean");
    }

    private static final String SEPARATOR = File.separator;

    public ProtostuffSerializationClassProcessor() {
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        processingEnv.getMessager().printMessage(Kind.NOTE, "ProtostuffSerializationClassProcessor init");
        System.out.println("ProtostuffSerializationClassProcessor init");
        super.init(processingEnv);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        System.out.println("正在进行 ProtostuffSerializationClass 类检查");
        for (TypeElement annotatedElement : ElementFilter.typesIn(roundEnv.getElementsAnnotatedWith(ProtostuffSerializationClass.class))) {
            if (annotatedElement.getKind() != ElementKind.CLASS) {
                processingEnv.getMessager().printMessage(Kind.ERROR, "ProtostuffSerializationClass 必须标记在类的上");
                return false;
            }
            try {
                checkForChanges(annotatedElement);
            } catch (IOException e) {
                processingEnv.getMessager().printMessage(Kind.ERROR, "构建出现错误，请进行检查");
                e.printStackTrace();
            }
        }
        System.out.println("ProtostuffSerializationClass 类检查通过");
        return true;
    }

    private void checkForChanges(TypeElement annotatedElement) throws IOException {
        // TODO: 在这里编写检查类结构变化的逻辑
        // 比较原始类结构与当前类结构，如果有修改，生成编译时警告
        String className = annotatedElement.getQualifiedName().toString();

        System.out.println("正在对[" + className + "]类进行检查");

        List<ProtostuffSerializationSchema> newSchemaList = new ArrayList<>();
        List<? extends Element> elements = annotatedElement.getEnclosedElements();
        Map<String, ? extends Element> nameMap = elements.stream()
            .collect(Collectors.toMap(element -> element.getSimpleName().toString(), Function.identity()));
        for (int i = 0; i < elements.size(); i++) {
            Element element = elements.get(i);
            if (element.getKind() != ElementKind.FIELD) {
                continue;
            }
            String fieldTypeName = element.asType().toString();
            String primitiveType = TYPE_MAPPING.get(fieldTypeName);
            if (primitiveType == null) {
                String getMethodName = "get" + element.getSimpleName().toString().substring(0, 1).toUpperCase() + element.getSimpleName().toString().substring(1);
                ExecutableElement method = (ExecutableElement) nameMap.get(getMethodName);
                primitiveType = method.getReturnType().toString();
            }

            ProtostuffSerializationSchema schema = ProtostuffSerializationSchema.builder()
                .fieldName(element.getSimpleName().toString())
                .fieldType(primitiveType)
                .fieldIndex(i)
                .build();
            newSchemaList.add(schema);
        }
        ProtostuffSerializationClass annotation =  annotatedElement.getAnnotation(ProtostuffSerializationClass.class);
        String schemaRelativePath = getFilePath(annotation.configPath(), className);
        Path schemaPath = Paths.get(schemaRelativePath);
        if (!Files.exists(schemaPath)) {
            if (!annotation.firstGenerate()) {
                processingEnv.getMessager().printMessage(Kind.ERROR, className + "配置文件不存在，请进行检查");
                return;
            }

            writeFile(annotation.configPath(), className, newSchemaList);
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (String line : Files.readAllLines(schemaPath)) {
            sb.append(line);
        }
        // 对比新的类文件和老的类文件，是否有字段类型，顺序的修改和删除
        List<ProtostuffSerializationSchema> localSerializationSchemaList = JSON.parseArray(sb.toString(), ProtostuffSerializationSchema.class);

        boolean illegal = compare(localSerializationSchemaList, newSchemaList, processingEnv);
        if (!illegal) {
            processingEnv.getMessager().printMessage(Kind.ERROR, "类 " + className + " 的结构可能影响数据兼容性");
            return;
        }
        writeFile(annotation.configPath(), className, newSchemaList);
        System.out.println("[" + className + "]类检查完成");
    }

    private void writeFile(String configPath, String className, List<ProtostuffSerializationSchema> newSchemaList)
        throws IOException {
        System.out.println("正在生成对比文件:[" + className + "]······");
        // 将新配置添加到 resource 目录下
        String newSchemaJson = JSON.toJSONString(newSchemaList);
        String schemaPath = getFilePath(configPath, className);
        Path path = Paths.get(schemaPath);
        // 创建目录
        if (!Files.exists(path.getParent())) {
            Files.createDirectories(path.getParent());
        }
        // 创建文件
        if (!Files.exists(path)) {
            Files.createFile(path);
        }
        Files.write(path, newSchemaJson.getBytes(StandardCharsets.UTF_8));
    }

    private String getFilePath(String configPath, String className) {
        String realPath = configPath.replace("/", SEPARATOR);
        String buildPath = System.getProperty("user.dir");
        String path;
        // 兼容多模块中编译指定的模块
        if (!configPath.equals(SEPARATOR) && buildPath.contains(configPath.substring(0, configPath.length() - 1))) {
            path = buildPath + SEPARATOR + "src" + SEPARATOR + "main" + SEPARATOR + "resources" + SEPARATOR + className + ".json";
        } else {
            path = buildPath + realPath + "src" + SEPARATOR + "main" + SEPARATOR + "resources" + SEPARATOR + className + ".json";
        }
        return path;
    }

    /**
     * 对比本地的配置和类结构中的配置
     *
     * @param local
     * @param newData
     * @param processingEnv
     * @return
     */
    private static boolean compare(List<ProtostuffSerializationSchema> local, List<ProtostuffSerializationSchema> newData, ProcessingEnvironment processingEnv) {
        if (local == null || newData == null) {
            processingEnv.getMessager().printMessage(Kind.ERROR, "数据为空，无法对比");
            return false;
        }
        // 不允许删除字段
        if (local.size() > newData.size()) {
            processingEnv.getMessager().printMessage(Kind.ERROR, "禁止删除 ProtostuffSerializationClass 标记的类的字段");
            return false;
        }
        // 不允许修改字段类型
        // 不允许修改字段顺序
        for (int i = 0; i < newData.size(); i++) {
            if (local.size() - 1 < i) {
                break;
            }
            ProtostuffSerializationSchema oldSchema = local.get(i);
            ProtostuffSerializationSchema newSchema = newData.get(i);
            if (Objects.isNull(oldSchema) || Objects.isNull(newSchema)) {
                processingEnv.getMessager().printMessage(Kind.ERROR, "字段为空，无法进行判断");
                return false;
            }
            if (!oldSchema.getFieldIndex().equals(newSchema.getFieldIndex())) {
                processingEnv.getMessager().printMessage(Kind.ERROR, "不允许修改字段顺序, fileName:" + oldSchema.getFieldName());
                return false;
            }
            if (!oldSchema.getFieldName().equals(newSchema.getFieldName())) {
                processingEnv.getMessager().printMessage(Kind.ERROR, "不允许修改字段名字, fileName:" + oldSchema.getFieldName());
                return false;
            }
            if (!oldSchema.getFieldType().equals(newSchema.getFieldType())) {
                processingEnv.getMessager().printMessage(Kind.ERROR, "不允许修改字段类型, fileName:" + oldSchema.getFieldName());
                return false;
            }
        }
        return true;
    }
}
