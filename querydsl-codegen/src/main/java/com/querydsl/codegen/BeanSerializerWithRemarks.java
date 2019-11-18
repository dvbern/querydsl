package com.querydsl.codegen;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Generated;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.mysema.codegen.CodeWriter;
import com.mysema.codegen.model.ClassType;
import com.mysema.codegen.model.Parameter;
import com.mysema.codegen.model.SimpleType;
import com.mysema.codegen.model.Type;
import com.mysema.codegen.model.TypeCategory;
import com.mysema.codegen.model.Types;
import com.querydsl.core.util.BeanUtils;

public class BeanSerializerWithRemarks implements Serializer {

    private static final String IMPORT_PACKAGE = "ch.dvbern.honegger.b3000.b2000import";
    private static final String ENTITIES_PACKAGE = IMPORT_PACKAGE + ".entities";
    private static final String HELPER_PACKAGE = IMPORT_PACKAGE + ".helpers";

    private static final Function<Property, Parameter> propertyToParameter = new Function<Property, Parameter>() {
        @Override
        public Parameter apply(Property input) {
            return new Parameter(input.getName(), input.getType());
        }
    };

    private final boolean propertyAnnotations;

    private final List<Type> interfaces = Lists.newArrayList();

    private final String javadocSuffix;

    private boolean addToString, addFullConstructor;

    private boolean printSupertype = false;

    /**
     * Create a new BeanSerializer
     */
    public BeanSerializerWithRemarks() {
        this(true, " is a Querydsl bean type");
    }

    /**
     * Create a new BeanSerializer with the given javadoc suffix
     *
     * @param javadocSuffix suffix to be used after the simple name in class level javadoc
     */
    public BeanSerializerWithRemarks(String javadocSuffix) {
        this(true, javadocSuffix);
    }

    /**
     * Create a new BeanSerializer
     *
     * @param propertyAnnotations true, to serialize property annotations
     */
    public BeanSerializerWithRemarks(boolean propertyAnnotations) {
        this(propertyAnnotations, " is a Querydsl bean type");
    }

    /**
     * Create a new BeanSerializer
     *
     * @param propertyAnnotations true, to serialize property annotations
     * @param javadocSuffix suffix to be used after the simple name in class level javadoc
     */
    public BeanSerializerWithRemarks(boolean propertyAnnotations, String javadocSuffix) {
        this.propertyAnnotations = propertyAnnotations;
        this.javadocSuffix = javadocSuffix;

        // DVB configuration
        this.addFullConstructor = true;
        this.addToString = true;
        this.printSupertype = true;
        this.interfaces.add(new SimpleType(IMPORT_PACKAGE + ".B2000Entity",
                IMPORT_PACKAGE,
                "B2000Entity"));
    }

    @Override
    public void serialize(EntityType model, SerializerConfig serializerConfig,
            CodeWriter writer) throws IOException {
        String simpleName = model.getSimpleName();

        // package
        if (!model.getPackageName().isEmpty()) {
            writer.packageDecl(model.getPackageName());
        }

        // imports
        Set<String> importedClasses = getAnnotationTypes(model);
        for (Type iface : interfaces) {
            importedClasses.add(iface.getFullName());
        }
        importedClasses.add(Generated.class.getName());
        if (model.hasLists()) {
            importedClasses.add(List.class.getName());
        }
        if (model.hasCollections()) {
            importedClasses.add(Collection.class.getName());
        }
        if (model.hasSets()) {
            importedClasses.add(Set.class.getName());
        }
        if (model.hasMaps()) {
            importedClasses.add(Map.class.getName());
        }
        if (addToString && model.hasArrays()) {
            importedClasses.add(Arrays.class.getName());
        }
        writer.importClasses(importedClasses.toArray(new String[importedClasses.size()]));

        // javadoc
        List<String> javadocLines = buildJavadocLines(model, simpleName + javadocSuffix);
        writer.javadoc(javadocLines.toArray(new String[0]));

        // header
        for (Annotation annotation : model.getAnnotations()) {
            writer.annotation(annotation);
        }

        writer.line("@Generated(\"", getClass().getName(), "\")");

        if (!interfaces.isEmpty()) {
            Type superType = null;
            // String helperName = model.getSimpleName() + "Helper";
            // Type superType = new SimpleType(HELPER_PACKAGE + "." + helperName, HELPER_PACKAGE, helperName);
            if (printSupertype && model.getSuperType() != null) {
                superType = model.getSuperType().getType();
            }
            Type[] ifaces = interfaces.toArray(new Type[interfaces.size()]);
            writer.beginClass(model, superType, ifaces);
        } else if (printSupertype && model.getSuperType() != null) {
            writer.beginClass(model, model.getSuperType().getType());
        } else {
            // String helperName = model.getSimpleName() + "Helper";
            // Type superType = new SimpleType(HELPER_PACKAGE + "." + helperName, HELPER_PACKAGE, helperName);
            // writer.beginClass(model, superType);
			writer.beginClass(model);
        }

        bodyStart(model, writer);

        if (addFullConstructor) {
            addFullConstructor(model, writer);
        }

        // fields
        for (Property property : model.getProperties()) {
            List<String> remarks = buildJavadocLines(property);
            if (!remarks.isEmpty()) {
                writer.javadoc(remarks.toArray(new String[0]));
            }
            if (propertyAnnotations) {
                for (Annotation annotation : property.getAnnotations()) {
                    writer.annotation(annotation);
                }
            }
            writer.privateField(property.getType(), property.getEscapedName());
        }

        // accessors
        for (Property property : model.getProperties()) {
            String propertyName = property.getEscapedName();
            Parameter parameter = new Parameter(propertyName, property.getType());
            List<String> remarks = buildJavadocLines(property);

            // getter
            if (!remarks.isEmpty()) {
                writer.javadoc(remarks.toArray(new String[0]));
            }
            writer.beginPublicMethod(property.getType(), "get" + BeanUtils.capitalize(propertyName));
            writer.line("return ", propertyName, ";");
            writer.end();
            if (!remarks.isEmpty()) {
                String sanitizedRemark = sanitizeRemarkForProperty(remarks) + "_" + propertyName;
                writer.javadoc(extendWith(remarks, "original property: " + propertyName).toArray(new String[0]));
                writer.beginPublicMethod(property.getType(), "get" + BeanUtils.capitalize(sanitizedRemark));
                writer.line("return ", propertyName, ";");
                writer.end();
            }

            // setter
            if (!remarks.isEmpty()) {
                writer.javadoc(remarks.toArray(new String[0]));
            }
            writer.beginPublicMethod(Types.VOID, "set" + BeanUtils.capitalize(propertyName), parameter);
            writer.line("this.", propertyName, " = ", propertyName, ";");
            writer.end();
            if (!remarks.isEmpty()) {
                String sanitizedRemark = sanitizeRemarkForProperty(remarks) + "_" + propertyName;
                writer.javadoc(extendWith(remarks, "original property: " + propertyName).toArray(new String[0]));
                writer.beginPublicMethod(Types.VOID, "set" + BeanUtils.capitalize(sanitizedRemark), parameter);
                writer.line("this.", propertyName, " = ", propertyName, ";");
                writer.end();
            }
        }

        if (addToString) {
            addToString(model, writer);
        }

        bodyEnd(model, writer);

        writer.end();
    }

    private List<String> extendWith(List<String> original, String extension) {
        ArrayList<String> strings = new ArrayList<String>(original);
        strings.add(extension);
        return strings;
    }

    protected String sanitizeRemarkForProperty(List<String> remarks) {
        String clean = Joiner.on('_').join(remarks)
                .trim()
				.replaceAll("%", "Pct")
                .replaceAll("[^a-zA-Z0-9_]+", "_");
                // .replaceAll("^\\d+", "");

        return clean;
    }

    private List<String> buildJavadocLines(Property property) {
        ArrayList<String> comments = new ArrayList<String>();
        String propertyComments = (String) property.getData().get("remarks");
        if (propertyComments == null || propertyComments.isEmpty()) {
            return comments;
        }
        comments.add(propertyComments);
        return comments;
    }

    private List<String> buildJavadocLines(EntityType model, String generated) {
        ArrayList<String> comments = new ArrayList<String>();
        String entityComments = (String) model.getData().get("remarks");
        if (entityComments != null && !entityComments.isEmpty()) {
            comments.add(entityComments);
            comments.add("");
        }
        comments.add(generated);
        return comments;
    }

    protected void addFullConstructor(EntityType model, CodeWriter writer) throws IOException {
        //TODO: constructor @param javadoc from remarks

        // public empty constructor
        writer.beginConstructor();
        writer.end();

        // full constructor
        writer.beginConstructor(model.getProperties(), propertyToParameter);
        for (Property property : model.getProperties()) {
            writer.line("this.", property.getEscapedName(), " = ", property.getEscapedName(), ";");
        }
        writer.end();
    }

    protected void addToString(EntityType model, CodeWriter writer) throws IOException {
        writer.line("@Override");
        writer.beginPublicMethod(Types.STRING, "toString");
        StringBuilder builder = new StringBuilder();
        for (Property property : model.getProperties()) {
            String propertyName = property.getEscapedName();
            if (builder.length() > 0) {
                builder.append(" + \", ");
            } else {
                builder.append("\"");
            }
            builder.append(propertyName + " = \" + ");
            if (property.getType().getCategory() == TypeCategory.ARRAY) {
                builder.append("Arrays.toString(" + propertyName + ")");
            } else {
                builder.append(propertyName);
            }
        }
        writer.line(" return ", builder.toString(), ";");
        writer.end();
    }

    protected void bodyStart(EntityType model, CodeWriter writer) throws IOException {
        // template method
    }

    protected void bodyEnd(EntityType model, CodeWriter writer) throws IOException {
        // template method
    }

    private Set<String> getAnnotationTypes(EntityType model) {
        Set<String> imports = new HashSet<String>();
        for (Annotation annotation : model.getAnnotations()) {
            imports.add(annotation.annotationType().getName());
        }
        if (propertyAnnotations) {
            for (Property property : model.getProperties()) {
                for (Annotation annotation : property.getAnnotations()) {
                    imports.add(annotation.annotationType().getName());
                }
            }
        }
        return imports;
    }

    public void addInterface(Class<?> iface) {
        interfaces.add(new ClassType(iface));
    }

    public void addInterface(Type type) {
        interfaces.add(type);
    }

    public void setAddToString(boolean addToString) {
        this.addToString = addToString;
    }

    public void setAddFullConstructor(boolean addFullConstructor) {
        this.addFullConstructor = addFullConstructor;
    }

    public void setPrintSupertype(boolean printSupertype) {
        this.printSupertype = printSupertype;
    }

}
