package com.github.os72.protobuf.dynamic.check;

import com.alibaba.fastjson.JSON;
import com.google.auto.service.AutoService;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic.Kind;
import org.apache.commons.collections4.CollectionUtils;

/**
 * @author ironman
 * @date 2023/7/27 11:23
 * 如果编译不通过，请注释掉所有的注解，编译成功之后再把注解打开，再编译一次就行
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
                processingEnv.getMessager().printMessage(Kind.ERROR, "构建出现错误，请进行检查 " + e.getMessage());
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

        // 处理父类字段
        TypeMirror superClassType = annotatedElement.getSuperclass();
        int index = 0;
        while (superClassType != null && !superClassType.toString().equals(Object.class.getTypeName())) {
            // Process the superclass fields
            if (!(superClassType instanceof DeclaredType)) {
                break;
            }
            DeclaredType declaredSuperType = (DeclaredType) superClassType;
            TypeElement superClassElement = (TypeElement) declaredSuperType.asElement();
            List<? extends Element> enclosedElements = superClassElement.getEnclosedElements();
            for (Element element : enclosedElements) {
                if (element.getKind() != ElementKind.FIELD) {
                    continue;
                }
                ProtostuffSerializationSchema schema = ProtostuffSerializationSchema.builder()
                    .fieldName(element.getSimpleName().toString())
                    .fieldType(element.asType().toString())
                    .fieldIndex(index ++)
                    .build();
                newSchemaList.add(schema);
            }
            superClassType = superClassElement.getSuperclass();
        }

        List<? extends Element> elements = annotatedElement.getEnclosedElements();
        for (int i = 0; i < elements.size(); i++) {
            Element element = elements.get(i);
            if (element.getKind() != ElementKind.FIELD) {
                continue;
            }
            ProtostuffSerializationSchema schema = ProtostuffSerializationSchema.builder()
                .fieldName(element.getSimpleName().toString())
                .fieldType(element.asType().toString())
                .fieldIndex(index ++)
                .build();
            newSchemaList.add(schema);
        }
        ProtostuffSerializationClass annotation =  annotatedElement.getAnnotation(ProtostuffSerializationClass.class);
        String schemaRelativePath = getFilePath(annotation.configPath(), className);
        URL configResource = getClass().getClassLoader()
            .getResource(schemaRelativePath.substring(schemaRelativePath.lastIndexOf(SEPARATOR) + 1));
        File configFile;
        if (configResource == null) {
            configFile = new File("");
        } else {
            String classPath = "target" + SEPARATOR + "classes";
            String resourcePath = "src" + SEPARATOR + "main" + SEPARATOR + "resources";
            configFile = new File(configResource.getPath().replace(classPath, resourcePath));
        }
        Path schemaPath = Paths.get(schemaRelativePath);
        System.out.println("configResource:" + configResource);
        System.out.println("configFile:" + configFile);
        System.out.println("schemaPath:" + schemaPath);



        processingEnv.getMessager().printMessage(Kind.WARNING, "configResource:" + configResource + " " + (configResource == null));
        processingEnv.getMessager().printMessage(Kind.WARNING, "configFile:" + configFile + " " + configFile.exists());
        processingEnv.getMessager().printMessage(Kind.WARNING, "schemaPath:" + schemaPath + " " + Files.exists(schemaPath));

        if (!configFile.exists() || annotation.firstGenerate()) {
            if (!annotation.firstGenerate()) {
                processingEnv.getMessager().printMessage(Kind.ERROR, className + "配置文件不存在，请进行检查");
                return;
            }

            writeFile(annotation.configPath(), className, newSchemaList);
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (String line : Files.readAllLines(configFile.toPath())) {
            sb.append(line);
        }
        // 对比新的类文件和老的类文件，是否有字段类型，顺序的修改和删除
        ProtostuffSerializationFile file = JSON.parseObject(sb.toString(), ProtostuffSerializationFile.class);

        boolean illegal = compare(file.getSchemas(), newSchemaList, processingEnv);
        if (!illegal) {
            processingEnv.getMessager().printMessage(Kind.ERROR, "类 " + className + " 的结构可能影响数据兼容性");
            return;
        }
        // 对比变更
        UpdateListVo<ProtostuffSerializationSchema> updateListVo = UpdateUtils.split(file.getSchemas(), newSchemaList,
            (oldItem, newItem) -> oldItem.getFieldType().equals(newItem.getFieldType()) &&
                    oldItem.getFieldName().equals(newItem.getFieldName()) &&
                    oldItem.getFieldIndex().equals(newItem.getFieldIndex()));
        if (CollectionUtils.isEmpty(updateListVo.getDeleteList()) &&
            CollectionUtils.isEmpty(updateListVo.getInsertList())) {
            System.out.println("暂无字段更新，不进行重写···");
        } else {
            System.out.println("字段有更新，即将覆盖原配置···");
            writeFile(annotation.configPath(), className, newSchemaList);
        }
        System.out.println("[" + className + "]类检查完成");
    }

    private void writeFile(String configPath, String className, List<ProtostuffSerializationSchema> newSchemaList)
        throws IOException {
        System.out.println("正在生成对比文件:[" + className + "]······");
        // 将新配置添加到 resource 目录下
        ProtostuffSerializationFile protostuffSerializationFile = ProtostuffSerializationFile.builder()
            .version(String.valueOf(System.currentTimeMillis()))
            .schemas(newSchemaList)
            .build();
        String newSchemaJson = JSON.toJSONString(protostuffSerializationFile);
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
        System.out.println("对比文件生成完成:[" + className + "]······");
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
