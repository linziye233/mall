代码生成器的使用
生成部分
1.java/com/test/mall/mapper
2.java/com/test/mall/model
3.resources/com/test/mall/mapper

配置部分
1.CommentGenerator 自定义注释生成器
2.Generator 生成器启动类
3.generator.properties 配置数据库链接和密码
4.generatorConfig.xml 生成器配置

使用方式
修改generator.properties，启动Generator，生成代码

模块备注
生成实体类被其他模块使用，不需要复制到其他模块，属于基础模块