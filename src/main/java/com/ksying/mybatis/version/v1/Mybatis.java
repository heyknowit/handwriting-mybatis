package com.ksying.mybatis.version.v1;

import com.ksying.mybatis.pojo.User;
import com.ksying.mybatis.util.SimpleTypeRegistry;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Simple package jdbc function
 * Extract the db info and sql info into mybatis.properties
 *
 * @author <a href="jiace.ksying@gmail.com">ksying</a>
 * @version v1.0 , 2020/3/25 23:25
 */
public class Mybatis {
    private Properties properties = new Properties();

    public void loadProperties() {
        try {
            // 加载mybatis.properties配置文件
            InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("mybatis.properties");
            properties.load(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<Object> selectList(String statementId, Object paramObject) {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet rs = null;
        List<Object> results = new ArrayList<>();
        try {
            // 获取连接
            Class.forName(properties.getProperty("db.driver"));
            connection = DriverManager.getConnection(properties.getProperty("db.url"),
                    properties.getProperty("db.username"), properties.getProperty("db.password"));
            // 获取SQL
            String sql = properties.getProperty("db.sql." + statementId);
            // 获取预处理 statement
            preparedStatement = connection.prepareStatement(sql);
            // 设置参数，第一个参数为 sql 语句中参数的序号（从 1 开始），第二个参数为设置的
            // 先获取入参类型
            // 处理参数类型及其Class对象
            String paramType = properties.getProperty("db.sql." + statementId + ".parametertype");
            Class paramObjectClass = Class.forName(paramType);
            // 先判断入参类型(8种基本类型、String类型、各种集合类型、自定义的Java类型)
            if (SimpleTypeRegistry.isSimpleType(paramObjectClass)) {
                preparedStatement.setObject(1, paramObject);
            } else {
                // 自定义的Java类型
                // 此处需要遍历SQL语句中的参数列表
                String params = properties.getProperty("db.sql." + statementId + ".params");
                String[] paramArray = params.split(",");
                for (int i = 0; i < paramArray.length; i++) {
                    String param = paramArray[i];
                    // 根据列名获取入参对象的属性值，前提：列名和属性名称要一致
                    Field field = paramObjectClass.getDeclaredField(param);
                    field.setAccessible(true);
                    // 获取属性值
                    Object value = field.get(paramObject);
                    preparedStatement.setObject(i + 1, value);
                }
            }
            // 向数据库发出 sql 执行查询，查询出结果集
            rs = preparedStatement.executeQuery();
            // 获取要映射的结果类型
            // 获取返回类型及其Class对象
            String resultType = properties.getProperty("db.sql." + statementId + ".resultclassname");
            Class resultClass = Class.forName(resultType);
            // 处理返回结果
            Object result = null;
            while (rs.next()) {
                // 每一行对应一个对象
                result = resultClass.newInstance();
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();
                for (int i = 1; i <= columnCount; i++) {
                    // 获取结果集中列的名称
                    String columnName = metaData.getColumnName(i);
                    Field field = resultClass.getDeclaredField(columnName);
                    field.setAccessible(true);
                    field.set(result, rs.getObject(columnName));
                }
                results.add(result);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 释放资源
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    // TODO Auto-generated catch block e.printStackTrace();
                }
            }
        }
        return results;
    }

    @Test
    public void testQueryUserByName() {
        loadProperties();
        User user = new User();
        user.setName("laoliu");
        List<Object> list = selectList("queryUserByName", user);
        System.out.println(list);
    }
}
