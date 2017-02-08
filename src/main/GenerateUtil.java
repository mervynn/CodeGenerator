package main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mysql.jdbc.StringUtils;

/**
 *  自动生成MyBatis的实体类、实体映射XML文件、Mapper
 *
 * @author   HeMingwei
 * @date     2016-8-20
 * @version  v1.0
 */
public class GenerateUtil {
 
    /**
     **********************************使用前必读*******************
     **
     ** 使用前请将moduleName更改为自己模块的名称即可（一般情况下与数据库名一致），其他无须改动。
     **
     ***********************************************************
     */
    private final String type_char = "char";
 
    private final String type_date = "date";
 
    private final String type_timestamp = "timestamp";
 
    private final String type_int = "int";
 
    private final String type_bigint = "bigint";
 
    private final String type_text = "text";
 
    private final String type_bit = "bit";
 
    private final String type_decimal = "decimal";
 
    private final String type_blob = "blob";
 
    private final static String moduleName = "baoming"; // 对应模块名称（根据自己模块做相应调整!!!务必修改^_^）
 
    private final String bean_path = "d:/entity_bean";
 
    private final String mapper_path = "d:/entity_mapper";
    
    private final String service_path = "d:/entity_service";
    
    private final String controller_path = "d:/entity_controller";
    
    private final String view_path = "d:/entity_view";
    
    private final static String package_prefix = "com.xinyin.tech." + moduleName;

    // 实体类包
    private static String bean_package = package_prefix + ".model.vo";
 
    // DAO接口包
    private static String mapper_package = package_prefix + ".mapper";
    
    // 服务接口包
    private static String service_package = package_prefix + ".service";
    
    // 业务层异常包
    private static String exception_package = package_prefix + ".exception";
    
    // 控制层类包
    private static String controller_base_package = package_prefix + ".controller";
    
    // 工具类包
    private static String util_package = package_prefix + ".util";
 
    private final String driverName = "com.mysql.jdbc.Driver";
 
    private final String user = "root";
 
    private final String password = "root";
 
    private final String url = "jdbc:mysql://localhost:3306/" + moduleName + "?characterEncoding=utf8";
    
    private final String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
 
    private String tableName = null;
    
    private String tableComment = null;
 
    private String beanName = null;
 
    private String mapperName = null;
    
    // Controller URL 非api
    private String controller_url = null;
    
    private String controller_package = null;
    
    // 一级菜单英文名
    private String first_level_menu_enname = null;
 
    private Connection conn = null;
 
 
    /**
     * 初始化获取db连接
     * 
     * @throws ClassNotFoundException
     * @throws SQLException
     */
    private void init() throws ClassNotFoundException, SQLException {
        Class.forName(driverName);
        conn = DriverManager.getConnection(url, user, password);
    }
 
 
    /**
     *  获取所有的表
     *
     * @return
     * @throws SQLException 
     */
    private List<String> getTables() throws SQLException {
        List<String> tables = new ArrayList<String>();
        PreparedStatement pstate = conn.prepareStatement("show tables");
        ResultSet results = pstate.executeQuery();
        while ( results.next() ) {
            String tableName = results.getString(1);
            //          if ( tableName.toLowerCase().startsWith("yy_") ) {
            tables.add(tableName);
            //          }
        }
        return tables;
    }
 
    /**
     * 根据table名称设定beanName和mapper文件名称
     * 
     * @param table
     */
    private void processTable( String table ) {
        StringBuffer sb = new StringBuffer(table.length());
        String tableNew = table.toLowerCase();
        String[] tables = tableNew.split("_");
        String temp = null;
        for ( int i = 0 ; i < tables.length ; i++ ) {
            temp = tables[i].trim();
            sb.append(temp.substring(0, 1).toUpperCase()).append(temp.substring(1));
        }
        beanName = sb.toString();
        mapperName = beanName + "Mapper";
    }
 
 
    /**
     * 根据db字段类型 返回java字段类型
     * 
     * @param type
     * @return
     */
    private String processType( String type ) {
        if ( type.indexOf(type_char) > -1 ) {
            return "String";
        } else if ( type.indexOf(type_bigint) > -1 ) {
            return "Long";
        } else if ( type.indexOf(type_int) > -1 ) {
            return "Integer";
        } else if ( type.indexOf(type_date) > -1 ) {
            return "java.util.Date";
        } else if ( type.indexOf(type_text) > -1 ) {
            return "String";
        } else if ( type.indexOf(type_timestamp) > -1 ) {
            return "java.util.Date";
        } else if ( type.indexOf(type_bit) > -1 ) {
            return "Boolean";
        } else if ( type.indexOf(type_decimal) > -1 ) {
            return "java.math.BigDecimal";
        } else if ( type.indexOf(type_blob) > -1 ) {
            return "byte[]";
        }
        return null;
    }
 
 
    /**
     * 字段名称转java规范变量
     * 
     * @param field
     * @return
     */
    private String processField( String field ) {
        StringBuffer sb = new StringBuffer(field.length());
        //field = field.toLowerCase();
        String[] fields = field.split("_");
        String temp = null;
        sb.append(fields[0]);
        for ( int i = 1 ; i < fields.length ; i++ ) {
            temp = fields[i].trim();
            sb.append(temp.substring(0, 1).toUpperCase()).append(temp.substring(1));
        }
        return sb.toString();
    }
 
 
    /**
     *  将实体类名首字母改为小写
     *
     * @param beanName
     * @return 
     */
    private String processResultMapId( String beanName ) {
        return beanName.substring(0, 1).toLowerCase() + beanName.substring(1);
    }
    
    /**
     * 根据beanName返回Controller(非api)文件路径
     * 同时设定Controller(非api)包路径
     * 同时设定Controller的请求url
     *
     * @param beanName
     * @return 
     */
    private String processControllerPath( String beanName ) {
    	if(beanName.startsWith("Tt")){
    		first_level_menu_enname = "user";
    		this.controller_url = "/"+first_level_menu_enname+"/" + processClassName(beanName).toLowerCase();
    		controller_package = controller_base_package + "."+first_level_menu_enname+";";
    		return controller_path + "/"+first_level_menu_enname+"";
    	}else if(beanName.startsWith("Ts")){
    		first_level_menu_enname = "sys";
    		this.controller_url = "/"+first_level_menu_enname+"/" + processClassName(beanName).toLowerCase();
    		controller_package = controller_base_package + "."+first_level_menu_enname+";";
    		return controller_path + "/"+first_level_menu_enname+"";
    	}else if(beanName.startsWith("Tm")){
    		first_level_menu_enname = "operation";
    		this.controller_url = "/"+first_level_menu_enname+"/" + processClassName(beanName).toLowerCase();
    		controller_package = controller_base_package + "."+first_level_menu_enname+";";
    		return controller_path + "/"+first_level_menu_enname+"";
    	}else if(beanName.startsWith("Tl")){
    		first_level_menu_enname = "flow";
    		this.controller_url = "/"+first_level_menu_enname+"/" + processClassName(beanName).toLowerCase();
    		controller_package = controller_base_package + "."+first_level_menu_enname+";";
    		return controller_path + "/"+first_level_menu_enname+"";
    	}else if(beanName.startsWith("Tc")){
    		first_level_menu_enname = "basedata";
    		this.controller_url = "/"+first_level_menu_enname+"/" + processClassName(beanName).toLowerCase();
    		controller_package = controller_base_package + "."+first_level_menu_enname+";";
    		return controller_path + "/"+first_level_menu_enname+"";
    	}else if(beanName.startsWith("Th")){
    		first_level_menu_enname = "history";
    		this.controller_url = "/"+first_level_menu_enname+"/" + processClassName(beanName).toLowerCase();
    		controller_package = controller_base_package + "."+first_level_menu_enname+";";
    		return controller_path + "/"+first_level_menu_enname+"";
    	}else if(beanName.startsWith("Tz")){
    		first_level_menu_enname = "zip";
    		this.controller_url = "/"+first_level_menu_enname+"/" + processClassName(beanName).toLowerCase();
    		controller_package = controller_base_package + "."+first_level_menu_enname+";";
    		return controller_path + "/"+first_level_menu_enname+"";
    	}else{
    		first_level_menu_enname = "";
    		return controller_base_package;
    	}
    }
    
    /**
     * 去掉实体前缀生成类前缀
     * 
     * @param beanName
     * @return 
     */
    private String processClassName( String beanName ) {
        return beanName.substring(2, beanName.length());
    }
 
 
    /**
     *  构建类上面的注释
     *
     * @param bw
     * @param text
     * @return
     * @throws IOException 
     */
    private BufferedWriter buildClassComment( BufferedWriter bw, String text ) throws IOException {
    	writeln(bw, "");
    	writeln(bw, "/**");
    	writeln(bw, " * ");
    	writeln(bw, " * " + text);
    	writeln(bw, " * ");
    	writeln(bw, " * 创建时间： " + date);
    	writeln(bw, " * @author HeMingwei");
    	writeln(bw, " * @version 1.0");
    	writeln(bw, " **/");
        return bw;
    }
 
 
    /**
     *  构建方法上面的注释
     *
     * @param bw
     * @param text
     * @return
     * @throws IOException 
     */
    private BufferedWriter buildMethodComment( BufferedWriter bw, String text ) throws IOException {
    	writeln(bw, "");
    	writeln(bw, "\t/**");
    	writeln(bw, "\t * ");
    	writeln(bw, "\t * " + text);
    	writeln(bw, "\t * ");
    	writeln(bw, "\t * @author HeMingwei");
    	writeln(bw, "\t * @param");
    	writeln(bw, "\t * @return");
    	writeln(bw, "\t **/");
        return bw;
    }
    
    private void buildSQL( BufferedWriter bw, List<String> columns, List<String> types ) throws IOException {
        int size = columns.size();
        // 通用结果列
        writeln(bw,  "\t<!-- 通用查询结果列-->");
        writeln(bw,  "\t<sql id=\"Base_Column_List\">");
        write(bw,  "\t\t id,");
        for ( int i = 1 ; i < size ; i++ ) {
            if ( i != size - 1 ) {
            	write(bw,  "\t" + columns.get(i));
            	write(bw,  ",");
            }else{
            	writeln(bw,  "\t" + columns.get(i));
            }
        }
 
        writeln(bw,  "\t</sql>");
 
        // 查询全部数据（根据条件模糊匹配）
        writeln(bw,  "\t<!-- 查询（根据条件模糊过滤） -->");
        writeln(bw,  "\t<select id=\"selectBySelective\" resultMap=\""
                + processResultMapId(beanName) + "ResultMap\">");
        writeln(bw,  "\t\t SELECT");
        writeln(bw,  "\t\t <include refid=\"Base_Column_List\" />");
        writeln(bw,  "\t\t FROM " + tableName);
        writeln(bw,  "\t\t WHERE 1=1");
        String tempField = null;
        for ( int i = 0 ; i < size ; i++ ) {
            tempField = processField(columns.get(i));
            // 日期类型数据 特殊处理
            if(types.get(i).indexOf(type_date) > -1){
            	writeln(bw,  "\t\t\t<if test=\"" + tempField + "Str != null\">");
            	writeln(bw,  "\t\t\t\tand " + columns.get(i) + " like concat('%',#{" + processField(columns.get(i)) +  "Str,jdbcType=VARCHAR},'%')");
            }else{
            	writeln(bw,  "\t\t\t<if test=\"" + tempField + " != null\">");
            	writeln(bw,  "\t\t\t\tand " + columns.get(i) + " like concat('%',#{" + processField(columns.get(i)) +  ",jdbcType=VARCHAR},'%')");
            }
            writeln(bw,  "\t\t\t</if>");
        }
        writeln(bw,  "\t</select>");
        
        // 查询全部数据（根据条件精确匹配）
        writeln(bw,  "\t<!-- 查询（根据条件精确匹配） -->");
        writeln(bw,  "\t<select id=\"selectExactlyBySelective\" resultMap=\""
                + processResultMapId(beanName) + "ResultMap\">");
        writeln(bw,  "\t\t SELECT");
        writeln(bw,  "\t\t <include refid=\"Base_Column_List\" />");
        writeln(bw,  "\t\t FROM " + tableName);
        writeln(bw,  "\t\t WHERE 1=1");
        for ( int i = 0 ; i < size ; i++ ) {
            tempField = processField(columns.get(i));
            // 日期类型数据 特殊处理
            if(types.get(i).indexOf(type_date) > -1){
            	writeln(bw,  "\t\t\t<if test=\"" + tempField + "Str != null\">");
            	writeln(bw,  "\t\t\t\tand " + columns.get(i) + " = #{" + processField(columns.get(i)) +  "Str,jdbcType=VARCHAR}");
            }else{
            	writeln(bw,  "\t\t\t<if test=\"" + tempField + " != null\">");
            	writeln(bw,  "\t\t\t\tand " + columns.get(i) + " = #{" + processField(columns.get(i)) +  ",jdbcType=VARCHAR}");
            }
            writeln(bw,  "\t\t\t</if>");
        }
        writeln(bw,  "\t</select>");
        
        // 查询（根据主键ID查询）
        writeln(bw,  "\t<!-- 查询（根据主键ID查询） -->");
        writeln(bw,  "\t<select id=\"selectByPrimaryKey\" resultMap=\""
                + processResultMapId(beanName) + "ResultMap\" parameterType=\"java.lang." + processType(types.get(0)) + "\">");
        writeln(bw,  "\t\t SELECT");
        writeln(bw,  "\t\t <include refid=\"Base_Column_List\" />");
        writeln(bw,  "\t\t FROM " + tableName);
        writeln(bw,  "\t\t WHERE " + columns.get(0) + " = #{" + processField(columns.get(0)) + "}");
        writeln(bw,  "\t</select>");
        // 查询完
 
        // 删除（根据主键ID删除）
        writeln(bw,  "\t<!--删除：根据主键ID删除-->");
        writeln(bw,  "\t<delete id=\"deleteByPrimaryKey\" parameterType=\"java.lang." + processType(types.get(0)) + "\">");
        writeln(bw,  "\t\t DELETE FROM " + tableName);
        writeln(bw,  "\t\t WHERE " + columns.get(0) + " = #{" + processField(columns.get(0)) + "}");
        writeln(bw,  "\t</delete>");
        
        // 删除（根据匹配字段）
        writeln(bw,  "\t<!--删除：根据匹配字段-->");
        writeln(bw,  "\t<delete id=\"deleteBySelective\" parameterType=\"" + bean_package + "." + beanName + "\">");
        writeln(bw,  "\t\t DELETE FROM " + tableName);
        writeln(bw,  "\t\t WHERE 1=1");
        tempField = null;
        for ( int i = 0 ; i < size ; i++ ) {
            tempField = processField(columns.get(i));
            writeln(bw,  "\t\t\t<if test=\"" + tempField + " != null\">");
            writeln(bw,  "\t\t\t\tand " + columns.get(i) + " = #{" + processField(columns.get(i)) +  ", jdbcType=VARCHAR}");
            writeln(bw,  "\t\t\t</if>");
        }
        writeln(bw,  "\t</delete>");
        // 删除完
 
 
        // 添加insert方法
        writeln(bw,  "\t<!-- 添加 -->");
        writeln(bw,  "\t<insert id=\"insert\" parameterType=\"" + bean_package + "." + beanName + "\">");
        writeln(bw,  "\t\t INSERT INTO " + tableName);
        write(bw,  " \t\t(");
        for ( int i = 0 ; i < size ; i++ ) {
        	if(i == 0){
        		write(bw,  columns.get(i));
            	write(bw,  ",");
        	}else if ( i != size - 1 ) {
            	write(bw,  "\t" + columns.get(i));
            	write(bw,  ",");
            }else{
            	write(bw,  "\t" + columns.get(i));
            }
        }
        writeln(bw,  ") ");
        writeln(bw,  "\t\t VALUES ");
        write(bw,  " \t\t(");
        for ( int i = 0 ; i < size ; i++ ) {
            if ( i != size - 1 ) {
            	write(bw,  "#{" + processField(columns.get(i)) + "}");
            	write(bw,  ",");
            }else{
            	write(bw,  "#{" + processField(columns.get(i)) + "}");
            }
        }
        writeln(bw,  ") ");
        writeln(bw,  "\t</insert>");
        // 添加insert完
 
        //---------------  insert方法（匹配有值的字段）
        writeln(bw,  "\t<!-- 添加 （匹配有值的字段）-->");
        writeln(bw,  "\t<insert id=\"insertSelective\" parameterType=\"" + bean_package + "." + beanName + "\">");
        writeln(bw,  "\t\t INSERT INTO " + tableName);
        writeln(bw,  "\t\t <trim prefix=\"(\" suffix=\")\" suffixOverrides=\",\" >");
        
        for ( int i = 0 ; i < size ; i++ ) {
            tempField = processField(columns.get(i));
            writeln(bw,  "\t\t\t<if test=\"" + tempField + " != null\">");
            writeln(bw,  "\t\t\t\t " + columns.get(i) + ",");
            writeln(bw,  "\t\t\t</if>");
        }
        writeln(bw,  "\t\t </trim>");
        writeln(bw,  "\t\t <trim prefix=\"values (\" suffix=\")\" suffixOverrides=\",\" >");
        tempField = null;
        for ( int i = 0 ; i < size ; i++ ) {
            tempField = processField(columns.get(i));
            writeln(bw,  "\t\t\t<if test=\"" + tempField + "!=null\">");
            writeln(bw,  "\t\t\t\t #{" + tempField + "},");
            writeln(bw,  "\t\t\t</if>");
        }
 
        writeln(bw,  "\t\t </trim>");
        writeln(bw,  "\t</insert>");
        
        //---------------  完毕
 
        // 修改（修改有值的字段）
        writeln(bw,  "\t<!-- 修 改（修改有值的字段）-->");
        writeln(bw,  "\t<update id=\"updateByPrimaryKeySelective\" parameterType=\"" + bean_package + "." + beanName + "\">");
        writeln(bw,  "\t\t UPDATE " + tableName);
        writeln(bw,  " \t\t <set> ");
        tempField = null;
        for ( int i = 1 ; i < size ; i++ ) {
            tempField = processField(columns.get(i));
            writeln(bw,  "\t\t\t<if test=\"" + tempField + " != null\">");
            writeln(bw,  "\t\t\t\t " + columns.get(i) + " = #{" + tempField + "},");
            writeln(bw,  "\t\t\t</if>");
        }
 
        writeln(bw,  " \t\t </set>");
        writeln(bw,  "\t\t WHERE " + columns.get(0) + " = #{" + processField(columns.get(0)) + "}");
        writeln(bw,  "\t</update>");
        
        // update方法完毕
 
        // ----- 修改
        writeln(bw,  "\t<!-- 修 改-->");
        writeln(bw,  "\t<update id=\"updateByPrimaryKey\" parameterType=\"java.lang." + processType(types.get(0)) + "\">");
        writeln(bw,  "\t\t UPDATE " + tableName);
        writeln(bw,  "\t\t SET ");
 
        tempField = null;
        for ( int i = 1 ; i < size ; i++ ) {
            tempField = processField(columns.get(i));
            if ( i != size - 1 ) {
                writeln(bw,  "\t\t\t" + columns.get(i) + " = #{" + tempField + "},");
            }else{
            	writeln(bw,  "\t\t\t" + columns.get(i) + " = #{" + tempField + "}");
            }
        }
 
        writeln(bw,  "\t\t WHERE " + columns.get(0) + " = #{" + processField(columns.get(0)) + "}");
        writeln(bw,  "\t</update>");
        
    }
 
 
    /**
     *  获取所有的数据库表注释
     *
     * @return
     * @throws SQLException 
     */
    private Map<String, String> getTableComment() throws SQLException {
        Map<String, String> maps = new HashMap<String, String>();
        PreparedStatement pstate = conn.prepareStatement("show table status");
        ResultSet results = pstate.executeQuery();
        while ( results.next() ) {
            String tableName = results.getString("NAME");
            String comment = results.getString("COMMENT");
            maps.put(tableName, comment);
        }
        return maps;
    }
    
    /**
     * 写数据(结尾换行)
     * @throws IOException 
     * 
     */
    private void writeln(BufferedWriter bw, String content) throws IOException{
    	bw.write(content);
    	bw.newLine();
    }
    
    /**
     * 写数据(结尾不换行)
     * @throws IOException 
     * 
     */
    private void write(BufferedWriter bw, String content) throws IOException{
    	bw.write(content);
    }
 
    /**
     *  生成实体类
     *
     * @param columns
     * @param types
     * @param comments
     * @throws IOException 
     */
    private void buildEntityBean( List<String> columns, List<String> types, List<String> comments )
        throws IOException {
        File folder = new File(bean_path);
        if ( !folder.exists() ) {
            folder.mkdir();
        }
 
        File beanFile = new File(bean_path, beanName + ".java");
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(beanFile)));
        writeln(bw,  "package " + bean_package + ";");
        writeln(bw, "");
        writeln(bw,  "import java.io.Serializable;");
        writeln(bw, "");
        writeln(bw,  "import " + package_prefix + ".model.pojo.BaseEntity;");
        //writeln(bw,  "import lombok.Data;");
        //      writeln(bw,  "import javax.persistence.Entity;");
        buildClassComment(bw, tableComment);
        writeln(bw,  "@SuppressWarnings(\"serial\")");
        //      writeln(bw,  "@Entity");
        //writeln(bw,  "@Data");
        writeln(bw,  "public class " + beanName + " extends BaseEntity implements Serializable {");
        writeln(bw, "");
        int size = columns.size();
        for ( int i = 0 ; i < size ; i++ ) {
        	if(!StringUtils.isNullOrEmpty(comments.get(i))){
        		writeln(bw,  "\t/**" + comments.get(i) + "**/");
        	}
            writeln(bw,  "\tprivate " + processType(types.get(i)) + " " + processField(columns.get(i)) + ";");
            writeln(bw, "");
        }
        // 生成get 和 set方法
        String tempField = null;
        String _tempField = null;
        String tempType = null;
        for ( int i = 0 ; i < size ; i++ ) {
            tempType = processType(types.get(i));
            _tempField = processField(columns.get(i));
            tempField = _tempField.substring(0, 1).toUpperCase() + _tempField.substring(1);
            //          writeln(bw,  "\tpublic void set" + tempField + "(" + tempType + " _" + _tempField + "){");
            writeln(bw,  "\tpublic void set" + tempField + "(" + tempType + " " + _tempField + "){");
            //          writeln(bw,  "\t\tthis." + _tempField + "=_" + _tempField + ";");
            writeln(bw,  "\t\tthis." + _tempField + " = " + _tempField + ";");
            writeln(bw,  "\t}");
            writeln(bw, "");
            writeln(bw,  "\tpublic " + tempType + " get" + tempField + "(){");
            writeln(bw,  "\t\treturn this." + _tempField + ";");
            writeln(bw,  "\t}");
        }
        writeln(bw,  "}");
        bw.flush();
        bw.close();
    }
 
 
    /**
     *  构建Mapper文件
     *
     * @throws IOException 
     */
    private void buildMapper() throws IOException {
        File folder = new File(mapper_path);
        if ( !folder.exists() ) {
            folder.mkdirs();
        }
 
        File mapperFile = new File(mapper_path, mapperName + ".java");
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(mapperFile), "utf-8"));
        writeln(bw,  "package " + mapper_package + ";");
        writeln(bw, "");
        writeln(bw,  "import " + bean_package + "." + beanName + ";");
        writeln(bw, "");
        writeln(bw,  "import java.util.List;");
        buildClassComment(bw, mapperName + "数据库操作接口类");
        //      writeln(bw,  "public interface " + mapperName + " extends " + mapper_extends + "<" + beanName + "> {");
        writeln(bw,  "public interface " + mapperName + "{");
        // ----------定义Mapper中的方法Begin----------
        
        buildMethodComment(bw, "查询（模糊匹配有值的字段）");
        writeln(bw,  "\tList<" + beanName + "> selectBySelective ( " + beanName + " record );");
        buildMethodComment(bw, "查询（精确匹配有值的字段）");
        writeln(bw,  "\tList<" + beanName + "> selectExactlyBySelective ( " + beanName + " record );");
        buildMethodComment(bw, "查询（根据主键ID查询）");
        writeln(bw,  "\t" + beanName + " selectByPrimaryKey ( String id );");
        buildMethodComment(bw, "删除（根据主键ID删除）");
        writeln(bw,  "\t" + "int deleteByPrimaryKey ( String id );");
        buildMethodComment(bw, "删除（匹配有值的字段）");
        writeln(bw,  "\t" + "int deleteBySelective ( " + beanName + " record );");
        buildMethodComment(bw, "添加");
        writeln(bw,  "\t" + "int insert( " + beanName + " record );");
        buildMethodComment(bw, "添加 （匹配有值的字段）");
        writeln(bw,  "\t" + "int insertSelective( " + beanName + " record );");
        buildMethodComment(bw, "修改 （修改有值的字段）");
        writeln(bw,  "\t" + "int updateByPrimaryKeySelective( " + beanName + " record );");
        buildMethodComment(bw, "修改（根据主键ID修改）");
        writeln(bw,  "\t" + "int updateByPrimaryKey ( " + beanName + " record );");
 
        // ----------定义Mapper中的方法End----------
        writeln(bw,  "}");
        bw.flush();
        bw.close();
    }
    
    /**
     *  构建实体类映射XML文件
     *
     * @param columns
     * @param types
     * @param comments
     * @throws IOException 
     */
    private void buildMapperXml( List<String> columns, List<String> types, List<String> comments ) throws IOException {
        File folder = new File(mapper_path + "/xml");
        if ( !folder.exists() ) {
            folder.mkdirs();
        }
 
        File mapperXmlFile = new File(mapper_path + "/xml", mapperName + ".xml");
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(mapperXmlFile)));
        writeln(bw,  "<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        writeln(bw,  "<!DOCTYPE mapper PUBLIC \"-//mybatis.org//DTD Mapper 3.0//EN\" ");
        writeln(bw,  "    \"http://mybatis.org/dtd/mybatis-3-mapper.dtd\">");
        writeln(bw,  "<mapper namespace=\"" + mapper_package + "." + mapperName + "\">");
 
        writeln(bw,  "\t<!--实体映射-->");
        writeln(bw,  "\t<resultMap id=\"" + this.processResultMapId(beanName) + "ResultMap\" type=\"" + bean_package + "." + beanName + "\">");
        writeln(bw,  "\t\t<!--" + comments.get(0) + "-->");
        writeln(bw,  "\t\t<id property=\"" + this.processField(columns.get(0)) + "\" column=\"" + columns.get(0) + "\" />");
        int size = columns.size();
        for ( int i = 1 ; i < size ; i++ ) {
        	if(!StringUtils.isNullOrEmpty(comments.get(i))){
        		writeln(bw,  "\t\t<!--" + comments.get(i) + "-->");
        	}
            writeln(bw,  "\t\t<result property=\""
                    + this.processField(columns.get(i)) + "\" column=\"" + columns.get(i) + "\" />");
        }
        writeln(bw,  "\t</resultMap>");
 
        // 下面开始写SqlMapper中的方法
        // this.outputSqlMapperMethod(bw, columns, types);
        buildSQL(bw, columns, types);
 
        writeln(bw,  "</mapper>");
        bw.flush();
        bw.close();
    }
    
    /**
     *  构建业务层接口文件
     *
     * @throws IOException 
     */
    private void buildService() throws IOException {
        File folder = new File(service_path);
        if ( !folder.exists() ) {
            folder.mkdirs();
        }
 
        File mapperFile = new File(service_path, "I" + processClassName(beanName) + "Service.java");
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(mapperFile), "utf-8"));
        writeln(bw,  "package " + service_package + ";");
        writeln(bw, "");
        writeln(bw,  "import java.util.List;");
        writeln(bw, "");
        writeln(bw,  "import com.github.miemiedev.mybatis.paginator.domain.PageList;");
        writeln(bw,  "import " + exception_package + ".ServiceException;");
        writeln(bw,  "import " + bean_package + "." + beanName + ";");
        buildClassComment(bw, tableComment + "信息管理服务层");
        //      writeln(bw,  "public interface " + mapperName + " extends " + mapper_extends + "<" + beanName + "> {");
        writeln(bw,  "public interface " + "I" + processClassName(beanName) + "Service" + "{");
        // ----------定义Service中的方法Begin----------
        buildMethodComment(bw, "查询（模糊匹配有值的字段）");
        writeln(bw,  "\tPageList<" + beanName + "> selectBySelective( " + beanName + " record ) throws ServiceException;");
        buildMethodComment(bw, "查询（精确匹配有值的字段）");
        writeln(bw,  "\tList<" + beanName + "> selectExactlyBySelective( " + beanName + " record ) throws ServiceException;");
        buildMethodComment(bw, "查询（根据主键ID查询）");
        writeln(bw,  "\t" + beanName + " selectByPrimaryKey( String id ) throws ServiceException;");
        buildMethodComment(bw, "删除（根据主键ID删除）");
        writeln(bw,  "\t" + "String deleteByPrimaryKey( String id ) throws ServiceException;");
        buildMethodComment(bw, "删除（匹配有值的字段）");
        writeln(bw,  "\t" + "String deleteBySelective ( " + beanName + " record ) throws ServiceException;");
        buildMethodComment(bw, "添加");
        writeln(bw,  "\t" + "String insert( " + beanName + " record ) throws ServiceException;");
        buildMethodComment(bw, "添加 （匹配有值的字段）");
        writeln(bw,  "\t" + "String insertSelective( " + beanName + " record ) throws ServiceException;");
        buildMethodComment(bw, "修改 （修改有值的字段）");
        writeln(bw,  "\t" + "String updateByPrimaryKeySelective( " + beanName + " record ) throws ServiceException;");
        buildMethodComment(bw, "修改（根据主键ID修改）");
        writeln(bw,  "\t" + "String updateByPrimaryKey( " + beanName + " record ) throws ServiceException;");
 
        // ----------定义Mapper中的方法End----------
        writeln(bw,  "}");
        bw.flush();
        bw.close();
    }
    
    /**
     *  构建业务层实现类
     *
     * @throws IOException 
     */
    private void buildServiceImpl( List<String> columns, List<String> types, List<String> comments ) throws IOException {
        File folder = new File(service_path + "/impl");
        if ( !folder.exists() ) {
            folder.mkdirs();
        }
 
        File mapperFile = new File(service_path + "/impl", processClassName(beanName) + "ServiceImpl.java");
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(mapperFile), "utf-8"));
        writeln(bw,  "package " + service_package + ".impl;");
        writeln(bw, "");
        writeln(bw,  "import java.util.Date;");
        writeln(bw,  "import java.util.HashMap;");
        writeln(bw,  "import java.util.List;");
        writeln(bw,  "import java.util.Map;");
        writeln(bw,  "");
        writeln(bw,  "import org.apache.commons.lang.StringUtils;");
        writeln(bw,  "import org.springframework.beans.factory.annotation.Autowired;");
        writeln(bw,  "import org.springframework.stereotype.Service;");
        writeln(bw,  "import org.springframework.transaction.annotation.Transactional;");
        writeln(bw,  "");
        writeln(bw,  "import com.alibaba.fastjson.JSONArray;");
        writeln(bw,  "import com.alibaba.fastjson.JSONObject;");
        writeln(bw,  "import com.github.miemiedev.mybatis.paginator.domain.PageList;");
        writeln(bw,  "import " + mapper_package + "." + beanName + "Mapper;");
        writeln(bw,  "import " + exception_package + ".ServiceException;");
        writeln(bw,  "import " + bean_package + "." + beanName + ";");
        writeln(bw,  "import " + service_package + ".I" + processClassName(beanName) + "Service;");
        writeln(bw,  "import " + util_package + ".Sequence;");
        writeln(bw,  "import " + util_package + ".Tools;");
        writeln(bw,  "import " + util_package + ".constant.Constant;");
        writeln(bw,  "import " + util_package + ".constant.Message;");
        buildClassComment(bw, tableComment + "信息管理服务层");
        writeln(bw,  "@Service");
        writeln(bw,  "@SuppressWarnings(\"serial\")");
        writeln(bw,  "public class " + processClassName(beanName) + "ServiceImpl" + " extends BaseService implements I" 
        		+ processClassName(beanName) + "Service {");
        writeln(bw, "");
        // ----------定义Service中的方法Begin----------
        writeln(bw,  "\t@Autowired");
        writeln(bw,  "\tprivate "+ beanName +"Mapper "+ processResultMapId(beanName)  +"Mapper;");
        writeln(bw, "");
        writeln(bw,  "\t@SuppressWarnings(\"unchecked\")");
        writeln(bw,  "\t@Override");
        writeln(bw,  "\tpublic PageList<" + beanName + ">  selectBySelective( " + beanName + " record ) throws ServiceException {");
        writeln(bw,  "\t\ttry {");
        writeln(bw,  "\t\t\tMap<String, Object> params =new HashMap<String, Object>();");
        int size = columns.size();
        for ( int i = 0 ; i < size ; i++ ) {
        	String _tempField = processField(columns.get(i));
            String tempField = _tempField.substring(0, 1).toUpperCase() + _tempField.substring(1);
            // 日期类型数据 特殊处理
            if(types.get(i).indexOf(type_date) > -1){
            	writeln(bw,  "\t\t\tparams.put(\""+ _tempField +"Str\",record.get" + tempField + "Str());");
            }else{
            	writeln(bw,  "\t\t\tparams.put(\""+ _tempField +"\",record.get" + tempField + "());");
            }
        }
        writeln(bw,  "\t\t\treturn (PageList<" + beanName + ">) getPageList(" + beanName + "Mapper.class, \"selectBySelective\",");
        writeln(bw,  "\t\t\t\tparams, record.getPage(), record.getPageSize(),record.getOrderSegment());");
        writeln(bw,  "\t\t}catch(Exception e){");
        writeln(bw,  "\t\t\tthrow new ServiceException(\""+ tableComment +"信息查询\",e);");
        writeln(bw,  "\t\t}");
        writeln(bw,  "\t}");
        
        buildMethodComment(bw, "查询（精确匹配有值字段）");
        writeln(bw, "\t@Override");
        writeln(bw,  "\tpublic List<" + beanName + "> selectExactlyBySelective( " + beanName + " record ) throws ServiceException {");
        writeln(bw, "\t\ttry {");
        writeln(bw, "\t\t\treturn " + processResultMapId(beanName)  + "Mapper.selectExactlyBySelective(record);");
        writeln(bw, "\t\t}catch(Exception e){");
        writeln(bw, "\t\t\tthrow new ServiceException(\"通过精确条件取得"+ tableComment +"信息\",e);");
        writeln(bw, "\t\t}");
        writeln(bw, "\t}");
        
        buildMethodComment(bw, "查询（根据主键ID查询）");
        writeln(bw, "\t@Override");
        writeln(bw,  "\tpublic " + beanName + " selectByPrimaryKey( String id ) throws ServiceException {");
        writeln(bw, "\t\ttry {");
        writeln(bw, "\t\t\treturn " + processResultMapId(beanName)  + "Mapper.selectByPrimaryKey(id);");
        writeln(bw, "\t\t}catch(Exception e){");
        writeln(bw, "\t\t\tthrow new ServiceException(\"通过ID取得"+ tableComment +"信息\",e);");
        writeln(bw, "\t\t}");
        writeln(bw, "\t}");
        
        buildMethodComment(bw, "删除（根据主键ID删除）");
        writeln(bw, "\t@Transactional");
        writeln(bw, "\t@Override");
        writeln(bw,  "\tpublic " + "String deleteByPrimaryKey( String id ) throws ServiceException {");
        writeln(bw, "\t\tString resMsg = StringUtils.EMPTY;");
        writeln(bw, "\t\ttry {");
        writeln(bw, "\t\t\tJSONArray arr =  JSONObject.parseArray(id);");
        writeln(bw, "\t\t\t// 拼装删除消息主键");
        writeln(bw, "\t\t\tString msgIds = StringUtils.EMPTY;");
        writeln(bw, "\t\t\tfor (int i = 0; i < arr.size(); i++) {");
        writeln(bw, "\t\t\t\tJSONObject jo = (JSONObject) arr.get(i);");
        writeln(bw, "\t\t\t\tString name = Tools.toString(jo.get(\"name\"));");
        writeln(bw, "\t\t\t\tString key = Tools.toString(jo.get(\"id\"));");
        writeln(bw, "\t\t\t\tint flag = " + processResultMapId(beanName)  + "Mapper.deleteByPrimaryKey(key);");
        writeln(bw, "\t\t\t\tif(!(flag > 0)){");
        writeln(bw, "\t\t\t\t\tthrow new ServiceException(Message.DELETE_FAILED);");
        writeln(bw, "\t\t\t\t}");
        writeln(bw, "\t\t\t\tmsgIds += StringUtils.isNotEmpty(msgIds)?Constant.COMMA+ name:name;");
        writeln(bw, "\t\t\t}");
        writeln(bw, "\t\t\tresMsg = \""+ tableComment +":\" + msgIds + Message.DELETE_SUCCESS;");
        writeln(bw, "\t\t}catch(Exception e){");
        writeln(bw, "\t\t\tthrow new ServiceException(\"" + tableComment +"信息删除\",e);");
        writeln(bw, "\t\t}");
        writeln(bw, "\t\treturn resMsg;");
        writeln(bw, "\t}");
        
        buildMethodComment(bw, "删除（根据匹配的字段）");
        writeln(bw, "\t@Transactional");
        writeln(bw, "\t@Override");
        writeln(bw,  "\tpublic " + "String deleteBySelective( " + beanName + " record ) throws ServiceException {");
        writeln(bw, "\t\tString resMsg = StringUtils.EMPTY;");
        writeln(bw, "\t\ttry {");
        writeln(bw, "\t\t\t" + processResultMapId(beanName)  + "Mapper.deleteBySelective(record);");
        writeln(bw, "\t\t}catch(Exception e){");
        writeln(bw, "\t\t\tthrow new ServiceException(\"" + tableComment +"信息删除\",e);");
        writeln(bw, "\t\t}");
        writeln(bw, "\t\treturn resMsg;");
        writeln(bw, "\t}");
        
        buildMethodComment(bw, "添加");
        writeln(bw,  "\t" + "@Transactional");
        writeln(bw,  "\t" + "@Override");
        writeln(bw,  "\tpublic " + "String insert( " + beanName + " record ) throws ServiceException {");
        writeln(bw,  "\t\t" + "String resMsg = StringUtils.EMPTY;");
        writeln(bw,  "\t\t" + "int flag = 0;");
        writeln(bw,  "\t\t" + "try{");
        writeln(bw,  "\t\t\t" + "record.setId(Sequence.nextId());");
        writeln(bw,  "\t\t\t" + "Date date = new Date();");
        writeln(bw,  "\t\t\t" + "record.setUpdateDate(date); // 更新时间");
        writeln(bw,  "\t\t\t" + "record.setCreateDate(date); // 创建时间");
        writeln(bw,  "\t\t\t" + "flag = " + processResultMapId(beanName)  + "Mapper.insert(record);");
        writeln(bw,  "\t\t\t" + "resMsg = flag > 0 ? Message.SAVE_SUCCESS : Message.SAVE_FAILED;");
        writeln(bw,  "\t\t" + "}catch(Exception e){");
        writeln(bw,  "\t\t\t" + "throw new ServiceException(\"" + tableComment +"信息添加\",e);");
        writeln(bw,  "\t\t" + "}");
        writeln(bw,  "\t\t" + "return resMsg;");
        writeln(bw,  "\t" + "}");
        
        buildMethodComment(bw, "添加 （匹配有值的字段）");
        writeln(bw,  "\t" + "@Transactional");
        writeln(bw,  "\t" + "@Override");
        writeln(bw,  "\tpublic " + "String insertSelective( " + beanName + " record ) throws ServiceException {");
        writeln(bw,  "\t\t" + "String resMsg = StringUtils.EMPTY;");
        writeln(bw,  "\t\t" + "int flag = 0;");
        writeln(bw,  "\t\t" + "try{");
        writeln(bw,  "\t\t\t" + "record.setId(Sequence.nextId());");
        writeln(bw,  "\t\t\t" + "Date date = new Date();");
        writeln(bw,  "\t\t\t" + "record.setUpdateDate(date); // 更新时间");
        writeln(bw,  "\t\t\t" + "record.setCreateDate(date); // 创建时间");
        writeln(bw,  "\t\t\t" + "flag = " + processResultMapId(beanName)  + "Mapper.insertSelective(record);");
        writeln(bw,  "\t\t\t" + "resMsg = flag > 0 ? Message.SAVE_SUCCESS : Message.SAVE_FAILED;");
        writeln(bw,  "\t\t" + "}catch(Exception e){");
        writeln(bw,  "\t\t\t" + "throw new ServiceException(\"" + tableComment +"信息添加\",e);");
        writeln(bw,  "\t\t" + "}");
        writeln(bw,  "\t\t" + "return resMsg;");
        writeln(bw,  "\t" + "}");
        buildMethodComment(bw, "修改 （修改有值的字段）");
        writeln(bw,  "\t" + "@Transactional");
        writeln(bw,  "\t" + "@Override");
        writeln(bw,  "\tpublic " + "String updateByPrimaryKeySelective( " + beanName + " record ) throws ServiceException {");
        writeln(bw,  "\t\t" + "String resMsg = StringUtils.EMPTY;");
		writeln(bw,  "\t\t" + "int flag = 0;");
        writeln(bw,  "\t\t" + "try{");
        writeln(bw,  "\t\t\t" + "Date date = new Date();");
        writeln(bw,  "\t\t\t" + "record.setUpdateDate(date); // 更新时间");
        writeln(bw,  "\t\t\t" + "flag = " + processResultMapId(beanName)  + "Mapper.updateByPrimaryKeySelective(record);");
        writeln(bw,  "\t\t\t" + "resMsg = flag > 0 ? Message.UPDATE_SUCCESS : Message.UPDATE_FAILED;");
        writeln(bw,  "\t\t" + "}catch(Exception e){");
        writeln(bw,  "\t\t\t" + "throw new ServiceException(\"" + tableComment +"信息修改\",e);");
        writeln(bw,  "\t\t" + "}");
        writeln(bw,  "\t\t" + "return resMsg;");
        writeln(bw,  "\t" + "}");
        buildMethodComment(bw, "修改（根据主键ID修改）");
        writeln(bw,  "\t" + "@Transactional");
        writeln(bw,  "\t" + "@Override");
        writeln(bw,  "\tpublic " + "String updateByPrimaryKey( " + beanName + " record ) throws ServiceException {");
		writeln(bw,  "\t\t" + "String resMsg = StringUtils.EMPTY;");
		writeln(bw,  "\t\t" + "int flag = 0;");
        writeln(bw,  "\t\t" + "try{");
        writeln(bw,  "\t\t\t" + "Date date = new Date();");
        writeln(bw,  "\t\t\t" + "record.setUpdateDate(date); // 更新时间");
        writeln(bw,  "\t\t\t" + "flag = " + processResultMapId(beanName)  + "Mapper.updateByPrimaryKey(record);");
        writeln(bw,  "\t\t\t" + "resMsg = flag > 0 ? Message.UPDATE_SUCCESS : Message.UPDATE_FAILED;");
        writeln(bw,  "\t\t" + "}catch(Exception e){");
        writeln(bw,  "\t\t\t" + "throw new ServiceException(\"" + tableComment +"信息修改\",e);");
        writeln(bw,  "\t\t" + "}");
        writeln(bw,  "\t\t" + "return resMsg;");
        writeln(bw,  "\t" + "}");
        // ----------定义Mapper中的方法End----------
        writeln(bw,  "}");
        bw.flush();
        bw.close();
    }
    
    /**
     *  构建控制层
     *
     * @throws IOException 
     */
    private void buildController( List<String> columns, List<String> types, List<String> comments ) throws IOException {
    	File folder = new File(processControllerPath(beanName));
        if ( !folder.exists() ) {
            folder.mkdirs();
        }
 
        File mapperFile = new File(processControllerPath(beanName), processClassName(beanName) + "Controller.java");
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(mapperFile), "utf-8"));
        writeln(bw,  "package " + controller_package);
        writeln(bw, "");
        
        writeln(bw, "import java.text.SimpleDateFormat;");
        writeln(bw, "import java.util.LinkedHashMap;");
        writeln(bw, "import java.util.Map;");
        writeln(bw, "");
        writeln(bw,  "import javax.servlet.http.HttpServletRequest;");
        writeln(bw,  "import javax.servlet.http.HttpServletResponse;");
        writeln(bw,  "import javax.servlet.http.HttpSession;");
        writeln(bw, "");
        writeln(bw,  "import org.apache.commons.lang.StringUtils;");
        writeln(bw,  "import org.apache.log4j.Logger;");
        writeln(bw,  "import org.springframework.beans.factory.annotation.Autowired;");
        writeln(bw,  "import org.springframework.stereotype.Controller;");
        writeln(bw,  "import org.springframework.web.bind.annotation.PathVariable;");
        writeln(bw,  "import org.springframework.web.bind.annotation.RequestMapping;");
        writeln(bw,  "import org.springframework.web.bind.annotation.RequestMethod;");
        writeln(bw,  "import org.springframework.web.bind.annotation.RequestParam;");
        writeln(bw,  "import org.springframework.web.bind.annotation.ResponseBody;");
        writeln(bw,  "");
        writeln(bw,  "import com.github.miemiedev.mybatis.paginator.domain.PageList;");
        writeln(bw,  "import " + package_prefix + ".controller.common.BaseController;");
        writeln(bw,  "import " + bean_package + "." + beanName + ";");
        writeln(bw,  "import " + bean_package + ".TsUser;");
        writeln(bw,  "import " + service_package + ".I" + processClassName(beanName) + "Service;");
        writeln(bw,  "import " + util_package + ".Tools;");
        writeln(bw,  "import " + util_package + ".constant.Constant;");
        writeln(bw,  "import " + util_package + ".constant.Views;");
        buildClassComment(bw, tableComment + "管理");
        writeln(bw,  "@Controller");
        writeln(bw,  "@RequestMapping(value = \""+ controller_url +"\")");
        writeln(bw,  "public class " + processClassName(beanName) + "Controller" + " extends BaseController {");
        writeln(bw, "");
        // ----------定义Controller中的方法Begin----------
        writeln(bw,  "\tprivate static final Logger logger = Logger.getLogger(" + processClassName(beanName) + "Controller.class);");
        writeln(bw, "");
        writeln(bw,  "\t@Autowired");
        writeln(bw,  "\tI"+ processClassName(beanName) +"Service "+ "i" + processClassName(beanName)  +"Service;");
        
        buildMethodComment(bw, tableComment + "管理初始化");
        writeln(bw,  "\t@RequestMapping()");
        writeln(bw,  "\tpublic String init() {");
        writeln(bw,  "\t\treturn Views."+ 
        (StringUtils.isNullOrEmpty(first_level_menu_enname)?"":first_level_menu_enname.toUpperCase()+"_")
        		+ processClassName(beanName).toUpperCase() +";");
        writeln(bw,  "\t}");
        buildMethodComment(bw, tableComment + "信息(返回Json数据)");
        writeln(bw,  "\t@RequestMapping(value = \"data\")");
        writeln(bw,  "\t@ResponseBody");
        writeln(bw,  "\tpublic Object[] query" + processClassName(beanName) + "JsonData("+ beanName +" record,");
        writeln(bw,  "\t\t\t@RequestParam(value=\"page\",required = true, defaultValue = Constant.DEFAULT_PAGE) int page,");
        writeln(bw,  "\t\t\t@RequestParam(value=\"size\",required = true, defaultValue = Constant.DEFAULT_PAGE_SIZE) int pageSize,");
        writeln(bw,  "\t\t\t@RequestParam(value=\"col[0]\",required = false, defaultValue = Constant.DESC_CODE) String sort0,");
        int size = columns.size();
        // 排序参数
        for ( int i = 1 ; i < size/2+1; i++ ) {
    		if(i < size/2 || (i == size/2 && size%2 == 1)){
    			writeln(bw,  "\t\t\t@RequestParam(value=\"col[" + (2*i-1) + "]\",required = false) String sort" + 
    					(2*i-1) +", @RequestParam(value=\"col[" + 2 * i + "]\",required = false) String sort" + 2 * i + ",");
    		}else{
    			writeln(bw,  "\t\t\t@RequestParam(value=\"col[" + (2*i-1) + "]\",required = false) String sort" + 
    					(2*i-1) +", @RequestParam(value=\"fcol[0]\", required = false) String "+ processField(columns.get(0)) +",");
    		}
        }
        // 查询条件参数
        for ( int i = 0 ; i < size/2; i++ ) {
        	if((size%2 == 0 && i < size/2-1) || (i == size/2 && size%2 == 1)){
    			writeln(bw,  "\t\t\t@RequestParam(value=\"fcol[" + (2*i+1) + "]\",required = false) String " + processField(columns.get(2*i+1))
    					+ ", @RequestParam(value=\"fcol[" + (2*i+2) + "]\",required = false) String " + processField(columns.get(2*i+2)) + ",");
        	}else if(size%2 == 1 && i < size/2){
        		writeln(bw,  "\t\t\t@RequestParam(value=\"fcol[" + (2*i) + "]\",required = false) String " + processField(columns.get((2*i)))
    					+ ", @RequestParam(value=\"fcol[" + (2*i+1) + "]\",required = false) String " + processField(columns.get(2*i+1)) + ",");
        	}
        }
		writeln(bw,  "\t\t\t@RequestParam(value=\"fcol[" + (size - 1) + "]\",required = false) String " + processField(columns.get(size - 1))
				+ ", HttpServletRequest req,HttpServletResponse resp) throws Exception{");
     	writeln(bw,  "\t\t// 返回数据集合");
     	writeln(bw,  "\t\tObject[] arr = new Object[2];");
        writeln(bw,  "\t\ttry {");
        writeln(bw,  "\t\t\t// 封装排序参数");
        
        writeln(bw,  "\t\t\tMap<String,String> map = new LinkedHashMap<String, String>(); // 先进先出");
        for ( int i = 1 ; i < size; i++ ) {
        	writeln(bw,  "\t\t\tmap.put(\""+ columns.get(i).toUpperCase() +"\", sort"+ i +");");
        }
        writeln(bw,  "\t\t\tmap.put(\""+ processField(columns.get(0)).toUpperCase() +"+1\", sort0);");
        writeln(bw,  "\t\t\trecord.setOrderSegment(Tools.toOrderSegment(map));");
        writeln(bw,  "\t\t\tPageList<"+ beanName +"> list = null;");
        writeln(bw,  "\t\t\tSimpleDateFormat sdf = new SimpleDateFormat(Constant.DATE_FORMAT);");
        // 分页参数
        writeln(bw,  "\t\t\trecord.setPage(page);");
        writeln(bw,  "\t\t\trecord.setPageSize(pageSize);");
        for ( int i = 0 ; i < size ; i++ ) {
        	String _tempField = processField(columns.get(i));
            String tempField = _tempField.substring(0, 1).toUpperCase() + _tempField.substring(1);
            // 日期类型数据 特殊处理
            if(types.get(i).indexOf(type_date) > -1){
            	writeln(bw,  "\t\t\trecord.set" + tempField + "Str("+ _tempField +");");
            }else{
            	writeln(bw,  "\t\t\trecord.set" + tempField + "("+_tempField+");");
            }
        }
        writeln(bw,  "\t\t\tlist = i" + processClassName(beanName)  +"Service.selectBySelective(record);");
        writeln(bw,  "\t\t\tObject[][] objs = new Object[list.size()]["+ size +"]; // 数组长度需要与前端列数保持一致");
        writeln(bw,  "\t\t\tfor(int i = 0;i<list.size();i++){");
        for ( int i = 0 ; i < size ; i++ ) {
        	String _tempField = processField(columns.get(i));
            String tempField = _tempField.substring(0, 1).toUpperCase() + _tempField.substring(1);
            // 日期类型数据 特殊处理
            if(types.get(i).indexOf(type_date) > -1){
            	writeln(bw,  "\t\t\t\tobjs[i]["+ i +"] = Tools.rowSelectionSet(list.get(i).get"+ tempField +"() !=null ?");
            	writeln(bw,  "\t\t\t\t\tsdf.format(list.get(i).get"+tempField+"()):StringUtils.EMPTY);");
            }else{
            	writeln(bw,  "\t\t\t\tobjs[i][" + i + "] = Tools.rowSelectionSet(list.get(i).get"+ tempField +"());");
            }
        }
        writeln(bw,  "\t\t\t}");
        writeln(bw,  "\t\t\tarr[0]=(list.getPaginator().getTotalCount());");
        writeln(bw,  "\t\t\tarr[1]=objs;");
        writeln(bw,  "\t\t} catch (Exception e) {");
        writeln(bw,  "\t\t\tlogger.error(Constant.SYSTEM_EXCEPTION, e);");
        writeln(bw,  "\t\t\tthrow e;");
        writeln(bw,  "\t\t}");
        writeln(bw,  "\t\treturn arr;");
        writeln(bw,  "\t}");
        
        buildMethodComment(bw, "添加" + tableComment + "信息");
        writeln(bw,  "\t@RequestMapping(value = \"save\", method=RequestMethod.POST)");
        writeln(bw,  "\t@ResponseBody");
        writeln(bw,  "\tpublic String save(HttpSession session, " + beanName + " record) {");
        writeln(bw,  "\t\tString resMsg = StringUtils.EMPTY;");
        writeln(bw,  "\t\ttry{");
        writeln(bw,  "\t\t\tTsUser user = (TsUser) session.getAttribute(Constant.LOGIN_KEY);");
        writeln(bw,  "\t\t\trecord.setCreateBy(user.getUsername());");
        writeln(bw,  "\t\t\trecord.setUpdateBy(user.getUsername());");
        writeln(bw,  "\t\t\tresMsg = i"+ processClassName(beanName) +"Service.insertSelective(record);");
        writeln(bw,  "\t\t}catch(Exception e){");
        writeln(bw,  "\t\t\tlogger.error(Constant.SYSTEM_EXCEPTION,e);");
        writeln(bw,  "\t\t\treturn Constant.SYSTEM_EXCEPTION;");
        writeln(bw,  "\t\t}");
        writeln(bw,  "\t\treturn resMsg;");
        writeln(bw,  "\t}");
        
        buildMethodComment(bw, "通过" + tableComment + "id取得"+ tableComment +"信息");
        writeln(bw,  "\t@RequestMapping(value = \"/data/{id}\")");
        writeln(bw,  "\t@ResponseBody");
        writeln(bw,  "\tpublic "+ beanName +" queryById(@PathVariable(\"id\") String id) {");
        writeln(bw,  "\t\t" + beanName +  " record = null;");
        writeln(bw,  "\t\ttry{");
        writeln(bw,  "\t\t\trecord = i"+ processClassName(beanName) +"Service.selectByPrimaryKey(id);");
        writeln(bw,  "\t\t}catch(Exception e){");
        writeln(bw,  "\t\t\tlogger.error(Constant.SYSTEM_EXCEPTION,e);");
        writeln(bw,  "\t\t}");
        writeln(bw,  "\t\treturn record;");
        writeln(bw,  "\t}");
        
        buildMethodComment(bw, "修改" + tableComment + "信息");
        writeln(bw,  "\t@RequestMapping(value = \"/modify\", method=RequestMethod.POST)");
        writeln(bw,  "\t@ResponseBody");
        writeln(bw,  "\tpublic String modify(HttpSession session, "+ beanName +" record) {");
        writeln(bw,  "\t\tString resMsg = StringUtils.EMPTY;");
        writeln(bw,  "\t\ttry{");
        writeln(bw,  "\t\t\tTsUser user = (TsUser) session.getAttribute(Constant.LOGIN_KEY);");
        writeln(bw,  "\t\t\trecord.setUpdateBy(user.getUsername());");
        writeln(bw,  "\t\t\tresMsg = i" + processClassName(beanName) + "Service.updateByPrimaryKeySelective(record);");
        writeln(bw,  "\t\t}catch(Exception e){");
        writeln(bw,  "\t\t\tlogger.error(Constant.SYSTEM_EXCEPTION,e);");
        writeln(bw,  "\t\t\treturn Constant.SYSTEM_EXCEPTION;");
        writeln(bw,  "\t\t}");
        writeln(bw,  "\t\treturn resMsg;");
        writeln(bw,  "\t}");

        buildMethodComment(bw, "根据id删除" + tableComment + "信息");
        writeln(bw,  "\t@RequestMapping(value = \"/delete\", method=RequestMethod.POST)");
        writeln(bw,  "\t@ResponseBody");
        writeln(bw,  "\tpublic String delete(@RequestParam(value=\"id\") String id) {");
        writeln(bw,  "\t\tString resMsg = StringUtils.EMPTY;");
        writeln(bw,  "\t\ttry{");
        writeln(bw,  "\t\t\tresMsg = i" + processClassName(beanName) + "Service.deleteByPrimaryKey(id);");
        writeln(bw,  "\t\t}catch(Exception e){");
        writeln(bw,  "\t\t\tlogger.error(Constant.SYSTEM_EXCEPTION,e);");
        writeln(bw,  "\t\t\treturn Constant.SYSTEM_EXCEPTION;");
        writeln(bw,  "\t\t}");
        writeln(bw,  "\t\treturn resMsg;");
        writeln(bw,  "\t}");
        
        // ----------定义Controller中的方法End----------
        writeln(bw,  "}");
        bw.flush();
        bw.close();
    }
    
    /**
     *  构建视图层
     *
     * @throws IOException 
     */
    private void buildView( List<String> columns, List<String> types, List<String> comments ) throws IOException {
    	// 通过该方法 设定view的文件夹
    	processControllerPath(beanName);
    	File folder = new File(view_path +  (StringUtils.isNullOrEmpty(first_level_menu_enname)?"":"/" + first_level_menu_enname));
        if ( !folder.exists() ) {
            folder.mkdirs();
        }
        File mapperFile = new File(view_path + (StringUtils.isNullOrEmpty(first_level_menu_enname)?"":"/" + first_level_menu_enname), 
        		processClassName(beanName).toLowerCase() + ".vm");
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(mapperFile), "utf-8"));
        writeln(bw, "#if($!session.getAttribute(\"HTTP_X_PJAX\") == \"true\")");
        writeln(bw, "\t#set($layout=\"/"+ (StringUtils.isNullOrEmpty(first_level_menu_enname)?""
        		: first_level_menu_enname + "/")  + processClassName(beanName).toLowerCase() +".vm\")");
        writeln(bw, "\t$session.setAttribute(\"HTTP_X_PJAX\",\"false\")");
        writeln(bw, "#end");
        writeln(bw, "<!-- TableSorter -->");
        writeln(bw, "<div id=\"panel-tablesorter\" class=\"panel panel-default\">");
        writeln(bw, "\t<div class=\"panel-heading bg-white\">");
        writeln(bw, "\t\t<div class=\"panel-actions\" >");
        writeln(bw, "\t\t\t<button  title=\"refresh\" class=\"btn-panel\" onclick=\"refresh();\" id=\"refresh\">");
        writeln(bw, "\t\t\t\t<i class=\"fa fa-refresh\"></i>");
        writeln(bw, "\t\t\t</button>");
        writeln(bw, "\t\t\t<button data-expand=\"#panel-tablesorter\" title=\"expand\" class=\"btn-panel\">");
        writeln(bw, "\t\t\t\t<i class=\"fa fa-expand\"></i>");
        writeln(bw, "\t\t\t</button>");
        writeln(bw, "\t\t\t<button data-collapse=\"#panel-tablesorter\" title=\"collapse\" class=\"btn-panel\">");
        writeln(bw, "\t\t\t\t<i class=\"fa fa-caret-down\"></i>");
        writeln(bw, "\t\t\t</button>");
        writeln(bw, "\t\t\t<button data-close=\"#panel-tablesorter\" title=\"close\" class=\"btn-panel\">");
        writeln(bw, "\t\t\t\t<i class=\"fa fa-times\"></i>");
        writeln(bw, "\t\t\t</button>");
        writeln(bw, "\t\t</div><!-- /panel-actions -->");
        writeln(bw, "\t\t<h3 class=\"panel-title\">&nbsp;</h3>");
        writeln(bw, "\t</div><!-- /panel-heading -->");
        writeln(bw, "\t<div class=\"panel-body\">");
        writeln(bw, "\t\t<div class=\"row\">");
        writeln(bw, "\t\t\t<div class=\"col-xs-12\">");
        writeln(bw, "\t\t\t\t<div class=\"pull-right\">");
		writeln(bw, "\t\t\t\t\t<button type=\"button\" class=\"btn btn-success\" id=\"btnChooseAll\" onclick=\"chooseAll();\">");
        writeln(bw, "\t\t\t\t\t\t全选 <i class=\"fa fa-check\"></i></button>");
        writeln(bw, "\t\t\t\t\t<button type=\"button\" class=\"btn btn-success\" id=\"btnAdd\" onclick=\"accessAddModule();\"");
        writeln(bw, "\t\t\t\t\t\tdata-toggle=\"modal\" href=\"#editModal\">");
        writeln(bw, "\t\t\t\t\t\t添加 <i class=\"fa fa-plus\"></i></button>");
        writeln(bw, "\t\t\t\t\t<button type=\"button\" class=\"btn btn-success\" id=\"btnEdit\" onclick=\"accessEditModule();\"");
        writeln(bw, "\t\t\t\t\t\tdata-toggle=\"modal\" href=\"#editModal\">");
        writeln(bw, "\t\t\t\t\t\t修改 <i class=\"fa fa-edit\"></i></button>");
        writeln(bw, "\t\t\t\t\t<button type=\"button\" class=\"btn btn-success\" id=\"btnDel\" onclick=\"doDel();\">");
        writeln(bw, "\t\t\t\t\t\t删除 <i class=\"fa fa-times\"></i></button>");
        writeln(bw, "\t\t\t\t</div>");
        writeln(bw, "\t\t\t</div>");
        writeln(bw, "\t\t\t<div class=\"col-xs-12\" style=\"height: 10px;\">");
        writeln(bw, "\t\t\t</div>");
        writeln(bw, "\t\t</div>");
        writeln(bw, "\t\t<div class=\"table-responsive table-responsive-datatables\">");
        writeln(bw, "\t\t\t<table class=\"tablesorter table table-hover table-bordered\">");
        writeln(bw, "\t\t\t\t<thead>");
        writeln(bw, "\t\t\t\t\t<tr>");
        int size = columns.size();
        for ( int i = 0 ; i < size ; i++ ) {
        	writeln(bw, "\t\t\t\t\t\t<th style=\"text-align:center;\">"+ comments.get(i) +"</th>");
        }
        writeln(bw, "\t\t\t\t\t</tr>");
        writeln(bw, "\t\t\t\t</thead><!--/thead-->");
        writeln(bw, "\t\t\t\t<tbody>");
        writeln(bw, "\t\t\t\t</tbody><!--/tbody-->");
        writeln(bw, "\t\t\t\t<tfoot>");
        writeln(bw, "\t\t\t\t\t<tr>");
        writeln(bw, "\t\t\t\t\t\t<th colspan=\"" + size + "\" class=\"ts-pager form-horizontal\">");
        writeln(bw, "\t\t\t\t\t\t\t#parse(\"/layout/page.vm\")");
        writeln(bw, "\t\t\t\t\t\t</th>");
        writeln(bw, "\t\t\t\t</tr>");
        writeln(bw, "\t\t\t\t</tfoot><!--/tfoot-->");
        writeln(bw, "\t\t\t</table><!--/table tools-->");
        writeln(bw, "\t\t</div><!--/table-responsive-->");
        writeln(bw, "\t\t<form id=\"editModal\" data-validate=\"form\" class=\"modal fade\" >");
        writeln(bw, "\t\t\t<input type=\"hidden\" name=\"id\"/>");
        writeln(bw, "\t\t\t<div class=\"modal-dialog\">");
        writeln(bw, "\t\t\t\t<div class=\"modal-content\">");
        writeln(bw, "\t\t\t\t\t<div class=\"modal-header\">");
        writeln(bw, "\t\t\t\t\t\t<button type=\"button\" class=\"close\" data-dismiss=\"modal\" aria-hidden=\"true\">&times;</button>");
        writeln(bw, "\t\t\t\t\t\t<h4 class=\"modal-title\"><strong></strong></h4>");
        writeln(bw, "\t\t\t\t\t</div>");
        writeln(bw, "\t\t\t\t\t<div class=\"modal-body\">");
        writeln(bw, "\t\t\t\t\t\t<div class=\"form-horizontal\">");
        for ( int i = 1 ; i < size; i++ ) {
        	if(!("create_by".equals(columns.get(i))
        			||"create_date".equals(columns.get(i))
        			||"update_by".equals(columns.get(i))
        			||"update_date".equals(columns.get(i)))){
        		writeln(bw, "\t\t\t\t\t\t\t<div class=\"form-group\">");
            	writeln(bw, "\t\t\t\t\t\t\t\t<label class=\"col-sm-3 control-label\" for=\"name\">"+ comments.get(i) +":</label>");
            	writeln(bw, "\t\t\t\t\t\t\t\t<div class=\"col-sm-7\">");
            	writeln(bw, "\t\t\t\t\t\t\t\t\t<input class=\"form-control\" type=\"text\" name=\""+ processField(columns.get(i)) +"\" />");
            	writeln(bw, "\t\t\t\t\t\t\t\t</div>");
            	writeln(bw, "\t\t\t\t\t\t\t</div>");
        	}
        }
        writeln(bw, "\t\t\t\t\t\t</div>");
        writeln(bw, "\t\t\t\t\t</div>");
        writeln(bw, "\t\t\t\t\t<div class=\"modal-footer\">");
        writeln(bw, "\t\t\t\t\t\t<button class=\"btn btn-cloud\" data-dismiss=\"modal\" >");
        writeln(bw, "\t\t\t\t\t取消 <i class=\"fa fa-mail-reply\"></i>");
        writeln(bw, "\t\t\t\t\t\t</button>");
        writeln(bw, "\t\t\t\t\t\t<button class=\"btn btn-success\" type=\"button\" >");
        writeln(bw, "\t\t\t\t\t保存 <i class=\"fa fa-check\"></i>");
        writeln(bw, "\t\t\t\t\t\t</button>");
        writeln(bw, "\t\t\t\t\t</div>");
        writeln(bw, "\t\t\t\t</div>");
        writeln(bw, "\t\t\t</div>");
        writeln(bw, "\t\t</form>");
        writeln(bw, "\t</div><!--/panel-body-->");
        writeln(bw, "</div><!--/panel-tablesorter-->");
        writeln(bw, "<script type=\"text/javascript\">");
        writeln(bw, "globleUrl = \"$request.getContextPath()"+ (StringUtils.isNullOrEmpty(first_level_menu_enname)?"":"/" + first_level_menu_enname) 
        		+"/"+ processClassName(beanName).toLowerCase() +"/\";");
        writeln(bw, "title = \""+ tableComment +"\";");
        write(bw, "");
        writeln(bw, "// 编辑窗口数据初始化-回调函数");
        writeln(bw, "function editInitCallBack(msg){");
        for ( int i = 0 ; i < size; i++ ) {
        	if(!("create_by".equals(columns.get(i))
        			||"create_date".equals(columns.get(i))
        			||"update_by".equals(columns.get(i))
        			||"update_date".equals(columns.get(i)))){
        		writeln(bw, "\t$(\"#editModal input[name='"+ processField(columns.get(i)) +"']\").val(msg."+processField(columns.get(i))+");");
        	}
        }
        writeln(bw, "}");
        writeln(bw, "");
        
        writeln(bw, "// 删除准备数据");
        writeln(bw, "function delDate(){");
        writeln(bw, "\tvar idArr=[];");
        writeln(bw, "\t$(\"table.tablesorter tbody tr\").each(function(){");
        writeln(bw, "\t\tvar id = $(this).find(\"td:eq(0)\").text();");
        writeln(bw, "\t\tvar name = $(this).find(\"td:eq(1)\").text();");
        writeln(bw, "\t\tif($(this).find(\"td:eq(0)\").css(\"background-color\") == colorRgb){");
        writeln(bw, "\t\t\tidArr.push({\"id\":id,\"name\":name});");
        writeln(bw, "\t\t}");
        writeln(bw, "\t});");
        writeln(bw, "\treturn idArr;");
        writeln(bw, "}");
        writeln(bw, "</script>");
        
        // ----------定义script中的方法End----------
        bw.flush();
        bw.close();
    	
    }
    
    /**
     *  构建api
     *
     * @throws IOException 
     */
    private void buildApi() throws IOException {
    	File folder = new File(controller_path + "/api/mobile");
        if ( !folder.exists() ) {
            folder.mkdirs();
        }
 
        File mapperFile = new File(controller_path + "/api/mobile", processClassName(beanName) + "ApiController.java");
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(mapperFile), "utf-8"));
        writeln(bw,  "package " + controller_base_package + ".api.mobile" + ";");
        writeln(bw, "");
        writeln(bw, "import java.util.LinkedHashMap;");
        writeln(bw, "import java.util.Map;");
        writeln(bw, "");
        writeln(bw,  "import javax.servlet.http.HttpServletRequest;");
        writeln(bw, "");
        writeln(bw,  "import org.apache.commons.lang.StringUtils;");
        writeln(bw,  "import org.apache.log4j.Logger;");
        writeln(bw,  "import org.springframework.beans.factory.annotation.Autowired;");
        writeln(bw,  "import org.springframework.stereotype.Controller;");
        writeln(bw,  "import org.springframework.web.bind.annotation.RequestMapping;");
        writeln(bw,  "import org.springframework.web.bind.annotation.RequestMethod;");
        writeln(bw,  "import org.springframework.web.bind.annotation.ResponseBody;");
        writeln(bw,  "");
        writeln(bw,  "import com.github.miemiedev.mybatis.paginator.domain.PageList;");
        writeln(bw,  "import " + package_prefix + ".controller.common.BaseController;");
        writeln(bw,  "import " + package_prefix + ".model.pojo.RestResp;");
        writeln(bw,  "import " + bean_package + "." + beanName + ";");
        writeln(bw,  "import " + service_package + ".I" + processClassName(beanName) + "Service;");
        writeln(bw,  "import " + util_package + ".Tools;");
        writeln(bw,  "import " + util_package + ".constant.Constant;");
        writeln(bw,  "import " + util_package + ".constant.Message;");
        buildClassComment(bw, tableComment + "Api");
        writeln(bw,  "@Controller");
        writeln(bw,  "@RequestMapping(value = \"/api/v1/user/"+ processClassName(beanName).toLowerCase() +"\")");
        writeln(bw,  "public class " + processClassName(beanName) + "ApiController" + " extends BaseController {");
        writeln(bw, "");
        // ----------定义Controller中的方法Begin----------
        writeln(bw,  "\tprivate static final Logger logger = Logger.getLogger(" + processClassName(beanName) + "ApiController.class);");
        writeln(bw, "");
        writeln(bw,  "\t@Autowired");
        writeln(bw,  "\tI"+ processClassName(beanName) +"Service "+ "i" + processClassName(beanName)  +"Service;");
        
        buildMethodComment(bw, tableComment + "-列表");
        writeln(bw,  "\t@RequestMapping(method = RequestMethod.GET)");
        writeln(bw,  "\t@ResponseBody");
        writeln(bw,  "\tpublic RestResp get("+ beanName +" record, HttpServletRequest request) {");
        writeln(bw,  "\t\tRestResp resp = new RestResp();");
        writeln(bw,  "\t\t// 分页参数");
        writeln(bw,  "\t\tInteger pno = request.getParameter(Constant.PAGE_NO) == null ? 1");
        writeln(bw,  "\t\t\t: Integer.parseInt(request.getParameter(Constant.PAGE_NO));");
        writeln(bw,  "\t\tInteger size = request.getParameter(Constant.PAGE_SIZE) == null ? 10");
        writeln(bw,  "\t\t\t: Integer.parseInt(request.getParameter(Constant.PAGE_SIZE));");
        writeln(bw,  "\t\trecord.setPage(pno);");
        writeln(bw,  "\t\trecord.setPageSize(size);");
        writeln(bw,  "\t\tMap<String,String> map = new LinkedHashMap<String, String>(); // 先进先出");
        writeln(bw,  "\t\tmap.put(\"ID+1\", Constant.DESC_CODE);");
        writeln(bw,  "\t\trecord.setOrderSegment(Tools.toOrderSegment(map));");
        writeln(bw,  "\t\tresp.setCode(Constant.API_SUCCESS_CODE);");
        writeln(bw,  "\t\tresp.setMsg(Message.API_CALL_SUCCESS);");
        writeln(bw,  "\t\ttry {");
        writeln(bw,  "\t\t\tPageList<" + beanName + "> "+ processResultMapId(processClassName(beanName)) +"List = "
        		+ "i" + processClassName(beanName)  +"Service.selectBySelective(record);");
        writeln(bw,  "\t\t\tresp.setData("+ processResultMapId(processClassName(beanName)) +"List);");
        writeln(bw,  "\t\t} catch (Exception e) {");
        writeln(bw,  "\t\t\tresp.setCode(Constant.API_EXCEPTION_CODE);");
        writeln(bw,  "\t\t\tresp.setMsg(Message.API_CALL_EXCEPTION);");
        writeln(bw,  "\t\t\tlogger.error(Message.API_CALL_EXCEPTION, e);");
        writeln(bw,  "\t\t}");
        writeln(bw,  "\t\treturn resp;");
        writeln(bw,  "\t}");
        
        buildMethodComment(bw, tableComment + "-添加");
        writeln(bw,  "\t@RequestMapping(method = RequestMethod.POST)");
        writeln(bw,  "\t@ResponseBody");
        writeln(bw,  "\tpublic RestResp post("+ beanName +" record, HttpServletRequest request) {");
        writeln(bw,  "\t\tRestResp resp = new RestResp();");
        writeln(bw,  "\t\tresp.setCode(Constant.API_SUCCESS_CODE);");
        writeln(bw,  "\t\tresp.setMsg(Message.API_CALL_SUCCESS);");
        writeln(bw,  "\t\ttry {");
        writeln(bw,  "\t\t\tString resMsg = "+ "i" + processClassName(beanName)  +"Service.insertSelective(record);");
        writeln(bw,  "\t\t\tif(resMsg.indexOf(Message.SAVE_SUCCESS) == -1){");
        writeln(bw,  "\t\t\t\tresp.setCode(Constant.API_EXCEPTION_CODE);");
        writeln(bw,  "\t\t\t\tresp.setMsg(resMsg);");
        writeln(bw,  "\t\t\t\treturn resp;");
        writeln(bw,  "\t\t\t}");
        writeln(bw,  "\t\t\tresp.setData(record);");
        writeln(bw,  "\t\t} catch (Exception e) {");
        writeln(bw,  "\t\t\tresp.setCode(Constant.API_EXCEPTION_CODE);");
        writeln(bw,  "\t\t\tresp.setMsg(Message.API_CALL_EXCEPTION);");
        writeln(bw,  "\t\t\tlogger.error(Message.API_CALL_EXCEPTION, e);");
        writeln(bw,  "\t\t}");
        writeln(bw,  "\t\treturn resp;");
        writeln(bw,  "\t}");
        
        buildMethodComment(bw, tableComment + "-变更");
        writeln(bw,  "\t@RequestMapping(method = RequestMethod.PUT)");
        writeln(bw,  "\t@ResponseBody");
        writeln(bw,  "\tpublic RestResp put("+ beanName +" record, HttpServletRequest request) {");
        writeln(bw,  "\t\tRestResp resp = new RestResp();");
        writeln(bw,  "\t\tresp.setCode(Constant.API_SUCCESS_CODE);");
        writeln(bw,  "\t\tresp.setMsg(Message.API_CALL_SUCCESS);");
        writeln(bw,  "\t\ttry {");
     	writeln(bw,  "\t\t\t// 主键非空判断");
     	writeln(bw,  "\t\t\tif(StringUtils.isBlank(record.getId())){");
     	writeln(bw,  "\t\t\t\tresp.setCode(Constant.API_ILLEGAL_PARAMETER_CODE);");
     	writeln(bw,  "\t\t\t\tresp.setMsg(Message.API_MISS_PRIMARY_KEY);");
     	writeln(bw,  "\t\t\t\treturn resp;");
     	writeln(bw,  "\t\t\t}");
        writeln(bw,  "\t\t\tString resMsg = "+ "i" + processClassName(beanName)  +"Service.updateByPrimaryKeySelective(record);");
        writeln(bw,  "\t\t\tif(resMsg.indexOf(Message.UPDATE_SUCCESS) == -1){");
        writeln(bw,  "\t\t\t\tresp.setCode(Constant.API_EXCEPTION_CODE);");
        writeln(bw,  "\t\t\t\tresp.setMsg(resMsg);");
        writeln(bw,  "\t\t\t\treturn resp;");
        writeln(bw,  "\t\t\t}");
        writeln(bw,  "\t\t\tresp.setData(record);");
        writeln(bw,  "\t\t} catch (Exception e) {");
        writeln(bw,  "\t\t\tresp.setCode(Constant.API_EXCEPTION_CODE);");
        writeln(bw,  "\t\t\tresp.setMsg(Message.API_CALL_EXCEPTION);");
        writeln(bw,  "\t\t\tlogger.error(Message.API_CALL_EXCEPTION, e);");
        writeln(bw,  "\t\t}");
        writeln(bw,  "\t\treturn resp;");
        writeln(bw,  "\t}");
        
        buildMethodComment(bw, tableComment + "-删除");
        writeln(bw,  "\t@RequestMapping(value = \"/delete\", method = RequestMethod.DELETE)");
        writeln(bw,  "\t@ResponseBody");
        writeln(bw,  "\tpublic RestResp delete("+ beanName +" record, HttpServletRequest request) {");
        writeln(bw,  "\t\tRestResp resp = new RestResp();");
        writeln(bw,  "\t\tresp.setCode(Constant.API_SUCCESS_CODE);");
        writeln(bw,  "\t\tresp.setMsg(Message.API_CALL_SUCCESS);");
        writeln(bw,  "\t\ttry {");
     	writeln(bw,  "\t\t\t// 主键非空判断");
     	writeln(bw,  "\t\t\tif(StringUtils.isBlank(record.getId())){");
     	writeln(bw,  "\t\t\t\tresp.setCode(Constant.API_ILLEGAL_PARAMETER_CODE);");
     	writeln(bw,  "\t\t\t\tresp.setMsg(Message.API_MISS_PRIMARY_KEY);");
     	writeln(bw,  "\t\t\t\treturn resp;");
     	writeln(bw,  "\t\t\t}");
        writeln(bw,  "\t\t\ti" + processClassName(beanName)  +"Service.deleteBySelective(record);");
        writeln(bw,  "\t\t} catch (Exception e) {");
        writeln(bw,  "\t\t\tresp.setCode(Constant.API_EXCEPTION_CODE);");
        writeln(bw,  "\t\t\tresp.setMsg(Message.API_CALL_EXCEPTION);");
        writeln(bw,  "\t\t\tlogger.error(Message.API_CALL_EXCEPTION, e);");
        writeln(bw,  "\t\t}");
        writeln(bw,  "\t\treturn resp;");
        writeln(bw,  "\t}");
        // ----------定义Api中的方法End----------
        writeln(bw,  "}");
        bw.flush();
        bw.close();
    	
    }
 
    /**
     * 生成各资源文件
     * 
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws IOException
     */
    public void generate() throws ClassNotFoundException, SQLException, IOException {
        init();
        String prefix = "show full fields from ";
        List<String> columns = null;
        List<String> types = null;
        List<String> comments = null;
        PreparedStatement pstate = null;
        List<String> tables = getTables();
        Map<String, String> tableComments = getTableComment();
        for ( String table : tables ) {
            columns = new ArrayList<String>();
            types = new ArrayList<String>();
            comments = new ArrayList<String>();
            pstate = conn.prepareStatement(prefix + table);
            ResultSet results = pstate.executeQuery();
            while ( results.next() ) {
                columns.add(results.getString("FIELD"));
                types.add(results.getString("TYPE"));
                comments.add(results.getString("COMMENT"));
            }
            tableName = table;
            processTable(table);
            //          this.outputBaseBean();
            tableComment = tableComments.get(tableName);
            buildEntityBean(columns, types, comments);
            buildMapper();
            buildMapperXml(columns, types, comments);
            buildService();
            buildServiceImpl(columns, types, comments);
            buildController(columns, types, comments);
            buildApi();
            buildView(columns, types, comments);
            
        }
        conn.close();
    }
    
    public static void main( String[] args ) {
        try {
            new GenerateUtil().generate();
            // 自动打开生成文件的目录
            Runtime.getRuntime().exec("cmd /c start explorer D:\\");
        } catch ( ClassNotFoundException e ) {
            e.printStackTrace();
        } catch ( SQLException e ) {
            e.printStackTrace();
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }
}
