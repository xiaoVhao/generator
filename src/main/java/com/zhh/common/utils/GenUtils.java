package com.zhh.common.utils;

import com.zhh.common.exception.BuzException;
import com.zhh.entity.ColumnEntity;
import com.zhh.entity.TableEntity;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;
import java.util.stream.IntStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 *
 * 代码生成器   工具类
 */
public class GenUtils {

    public static List<String> getTemplates(){
        List<String> templates = new ArrayList<String>();
        templates.add("template/Entity.java.vm");
        templates.add("template/Dao.java.vm");
        templates.add("template/Service.java.vm");
        templates.add("template/Index.java.vm");
        templates.add("template/ajax.jsp.vm");
        templates.add("template/aoru.jsp.vm");
        templates.add("template/list.jsp.vm");
        return templates;
    }

    /**
     * 生成代码
     */
    public static void generatorCode(Map<String, String> table,
                                     List<Map<String, String>> columns, ZipOutputStream zip) {
        //配置信息
        Properties config = getConfig();
        boolean hasBigDecimal = false;
        boolean hasCreateTime = false;
        //表信息
        TableEntity tableEntity = new TableEntity();
        tableEntity.setTableName(table.get("tableName" ));
        tableEntity.setComments(table.get("tableComment" ));
        //表名转换成Java类名
        String className = tableToJava(tableEntity.getTableName(), config.getProperty("tablePrefix" ));
        tableEntity.setClassName(className);
        tableEntity.setClassname(StringUtils.uncapitalize(className));

        //列信息
        List<ColumnEntity> columsList = new ArrayList<>();
        for(Map<String, String> column : columns){
            ColumnEntity columnEntity = new ColumnEntity();
            columnEntity.setColumnName(column.get("columnName" ));
            columnEntity.setDataType(column.get("dataType" ));
            columnEntity.setComments(column.get("columnComment" ));
            columnEntity.setExtra(column.get("extra" ));

            //列名转换成Java属性名
            String attrName = columnToJava(columnEntity.getColumnName());
            columnEntity.setAttrName(attrName);
            columnEntity.setAttrname(StringUtils.uncapitalize(attrName));

            //列的数据类型，转换成Java类型
            String attrType = config.getProperty(columnEntity.getDataType(), "unknowType" );
            columnEntity.setAttrType(attrType);
            if (!hasBigDecimal && attrType.equals("BigDecimal" )) {
                hasBigDecimal = true;
            }
            //是否主键
            if ("PRI".equalsIgnoreCase(column.get("columnKey" )) && tableEntity.getPk() == null) {
                tableEntity.setPk(columnEntity);
            }

            if(!hasCreateTime && "createTime".equalsIgnoreCase(columnEntity.getAttrName())){
                hasCreateTime = true;
            }

            columsList.add(columnEntity);
        }
        tableEntity.setColumns(columsList);

        //没主键，则第一个字段为主键
        if (tableEntity.getPk() == null) {
            tableEntity.setPk(tableEntity.getColumns().get(0));
        }

        //设置velocity资源加载器
        Properties prop = new Properties();
        prop.put("file.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader" );
        Velocity.init(prop);
        String mainPath = config.getProperty("mainPath" );
        mainPath = StringUtils.isBlank(mainPath) ? "com.zhh" : mainPath;
        //封装模板数据
        Map<String, Object> map = new HashMap<>();
        map.put("tableName", tableEntity.getTableName());
        map.put("comments", tableEntity.getComments());
        map.put("pk", tableEntity.getPk());
        map.put("className", tableEntity.getClassName());
        map.put("classname", tableEntity.getClassname());
        map.put("pathName", tableEntity.getClassname().toLowerCase());
        map.put("columns", tableEntity.getColumns());
        map.put("hasBigDecimal", hasBigDecimal);
        map.put("hasBigDecimal", hasBigDecimal);
        map.put("mainPath", mainPath);
        map.put("databaseName", config.getProperty("databaseName" ));
        map.put("package", config.getProperty("package" ));
        map.put("moduleName", config.getProperty("moduleName" ));
        map.put("author", config.getProperty("author" ));
        map.put("datetime", DateUtils.format(new Date(), DateUtils.DATE_TIME_PATTERN));
        map.put("insertSql",getInsertSql(tableEntity));
        map.put("updateSql",getUpdateSql(tableEntity));
        map.put("insertParam",getInsertParam(tableEntity));
        map.put("updateParam",getUpdateParam(tableEntity));
        map.put("dollC","$");
        VelocityContext context = new VelocityContext(map);

        //获取模板列表
        List<String> templates = getTemplates();
        for (String template : templates) {
            //渲染模板
            StringWriter sw = new StringWriter();
            Template tpl = Velocity.getTemplate(template, "UTF-8" );
            tpl.merge(context, sw);

            try {
                //添加到zip
                zip.putNextEntry(new ZipEntry(getFileName(template, tableEntity.getClassName(), config.getProperty("package" ), config.getProperty("moduleName" ))));
                IOUtils.write(sw.toString(), zip, "UTF-8" );
                IOUtils.closeQuietly(sw);
                zip.closeEntry();
            } catch (IOException e) {
                throw new BuzException("渲染模板失败，表名：" + tableEntity.getTableName(), e);
            }
        }
    }


    /**
     * 列名转换成Java属性名
     */
    public static String columnToJava(String columnName) {
        if(columnName != null && columnName.length() != 0 ) {
            int strLen = columnName.length();
            StringBuffer buffer = new StringBuffer(strLen);
            boolean uncapitalizeNext = false;

            for(int i = 0; i < strLen; ++i) {
                char ch = columnName.charAt(i);
                if(ch == '_') {
                    uncapitalizeNext = true;
                } else if(uncapitalizeNext) {
                    buffer.append(Character.toUpperCase(ch));
                    uncapitalizeNext = false;
                } else {
                    buffer.append(i==0 ? Character.toUpperCase(ch) : ch);
                }
            }

            return buffer.toString();
        } else {
            return columnName;
        }
    }

    /**
     * 表名转换成Java类名
     */
    public static String tableToJava(String tableName, String tablePrefix) {
        if (StringUtils.isNotBlank(tablePrefix)) {
            tableName = tableName.replace(tablePrefix, "" );
        }
        return columnToJava(tableName);
    }

    /**
     * 获取配置信息
     */
    public static Properties getConfig() {
        try {
            Properties properties = PropertiesLoaderUtils.loadAllProperties("generator.properties");
            return properties;
        } catch (Exception e) {
            throw new BuzException("获取配置文件失败，", e);
        }
    }

    /**
     * 获取文件名
     */
    public static String getFileName(String template, String className, String packageName, String moduleName) {
        String packagePath = "main" + File.separator + "java" + File.separator;
        if (StringUtils.isNotBlank(packageName)) {
            packagePath += packageName.replace(".", File.separator) + File.separator + moduleName + File.separator;
        }

        if (template.contains("Entity.java.vm" )) {
            return packagePath + "entity" + File.separator + className + ".java";
        }

        if (template.contains("Dao.java.vm" )) {
            return packagePath + "dao" + File.separator + className + "Dao.java";
        }

        if (template.contains("Service.java.vm" )) {
            return packagePath + "service" + File.separator + className + "Service.java";
        }

        if (template.contains("Index.java.vm" )) {
            return packagePath + "controller" + File.separator + className + "Index.java";
        }

        if (template.contains("ajax.jsp.vm" )) {
            return packagePath + "jsp" + File.separator + className + File.separator + "ajax.jsp";
        }

        if (template.contains("aoru.jsp.vm" )) {
            return packagePath + "jsp" + File.separator + className  + File.separator + "aoru.jsp";
        }

        if (template.contains("list.jsp.vm" )) {
            return packagePath + "jsp" + File.separator + className + File.separator + "list.jsp";
        }
        return null;
    }

    public static String getInsertSql(TableEntity tableEntity){
        StringBuilder sb = new StringBuilder("insert into ");
        sb.append("`").append(tableEntity.getTableName()).append("`(");

        tableEntity.getColumns().stream().forEach(
                column -> {
                    sb.append("`").append(column.getColumnName()).append("`,");
                }
        );

        sb.deleteCharAt(sb.length()-1);
        sb.append(" ) VALUES (");

        IntStream.range(0,tableEntity.getColumns().size()).forEach(i->{
            sb.append("?");
            if(i != tableEntity.getColumns().size() -1){
                sb.append(",");
            }
        });
        sb.append(")");

        return sb.toString();
    }

    public static String getUpdateSql(TableEntity tableEntity){
        StringBuilder sb = new StringBuilder("update  ");
        sb.append("`").append(tableEntity.getTableName()).append("` set ");

        tableEntity.getColumns().stream().forEach(
                column -> {
                    sb.append("`").append(column.getColumnName()).append("` = ?,");
                }
        );

        sb.deleteCharAt(sb.length()-1);
        sb.append("  where `").append(tableEntity.getClassname()).append(tableEntity.getPk().getColumnName()).append("` = ?") ;

        return sb.toString();
    }

    public static String getInsertParam(TableEntity tableEntity){

        StringBuilder sb = new StringBuilder("");
        tableEntity.getColumns().stream().forEach(
                column -> {
                    sb.append(tableEntity.getClassname()).append(".get").append(column.getAttrName()).append("(),");
                }
        );

        return sb.toString();
    }

    public static String getUpdateParam(TableEntity tableEntity){

       String str = getInsertParam(tableEntity);

        return str + " get"+tableEntity.getPk().getAttrName() + "()";
    }
}
