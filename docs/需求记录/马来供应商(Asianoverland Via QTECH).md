
##  马来供应商Asianoverland Via QTECH   （ 2025-09-04 ）

---
- 供应商技术通道官网：https://www.qtechsoftware.com/
- 联系人： Godrey Pereira
- 联系邮箱： 技术 <godrey.pereira@qtechsoftware.com> 、 客户经理  <halancia.deva@qtechsoftware.com>
- 联系电话：+91.22.46050602
- 类型：国际供应商 
- 供应商类型： 技术通道 （没有技术团队，通过技术通道放资源）
- 供应商标识： Asianoverland
---


### 接入方式

#### 测试API详情：
``` text
用户名： Heytrip_Test
密码： Welcome@@123
```

API接口地址

---
##### 1、hotel_search   酒店搜索
 - http://colosseum.otrams.com:8087/ws/index.php
##### 2、hotel_detail    酒店详情
 - https://colosseum.otrams.com/ws/index.php
##### 3、hotel_cancellation  酒店取消 
- https://colosseum.otrams.com/ws/index.php
##### 4、hotel_reservation   酒店预定
- https://colosseum.otrams.com/ws/index.php
##### 5、booking_detail  预定详情
- https://colosseum.otrams.com/ws/index.php
##### 6、get_cancellation_charges  获取取消费用
- https://colosseum.otrams.com/ws/index.php
##### 7、cancel_the_booking  取消预定
 - https://colosseum.otrams.com/ws/index.php

--- 

#### FTP访问服务
```
主机名：18.170.183.159
用户名：colosseum_live_static_data
密码：v7QAMfegDWcDBbqx

```
下载静态数据文件
```
static_data_cities.csv - 城市静态数据
static_data_countries.csv - 国家静态数据
static_data_hotels.csv - 酒店静态数据
static_data_nationality.csv - 国籍静态数据
```
注意：API对白名单的IP数量没有限制，但对于FTP，只能有两个IP被列入白名单。




### 过程分析 

- 需求分析：
    - 供应商通过QTECH技术通道提供酒店资源
    - 需要对接QTECH的API接口进行酒店搜索、预订、取消等操作
    - 需要处理API的认证、请求格式、响应解析等技术细节

- 技术要求：
    - 熟悉RESTful API设计和使用
    - 能够处理XML/JSON格式的数据交换
    - 具备基本的网络安全知识，确保数据传输的安全性

- 对接步骤：
    1. 获取API文档和测试账号
    2. 搭建测试环境，确保能够访问QTECH的测试服务器
    3. 实现API调用，包括酒店搜索、详情查询、预订、取消等功能
    4. 进行功能测试，确保各项功能正常运行
    5. 提交测试结果，安排认证流程
    6. 完成认证后，进行生产环境对接

- 注意事项：
    - 确认API的版本和更新频率，及时调整对接代码
    - 注意处理异常情况，如网络故障、API响应错误等
    - 保持与供应商的沟通，及时获取支持和帮助
- 交付成果：
    - 完成QTECH API的对接，实现酒店搜索、预订、取消等功能
    - 提供详细的技术文档，说明对接过程和注意事项
    - 确保系统稳定运行，能够处理实际业务需求
    - 提供后续的技术支持和维护，确保对接的持续有效性