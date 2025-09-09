
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


```
请注意以下限制：
    - 每笔预订的夜数：30晚
    - 每笔预订的客人数量：5间房 10位客人
    - 儿童年龄0-12岁
    - 每间房儿童数量：3名儿童
    - 所选服务日期不应超过365天
```



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

---
- QTECH API工作流程：
- 步骤 1 hotel_search 此 API 用于获取指定搜索的酒店 - 必选
- 步骤 2 hotel_detail 此 API 用于获取特定酒店的详细信息。 - 可选
- 步骤 3 hotel_cancellation_policy 此 API 用于获取酒店下特定房间的取消政策。 - 必选
- 步骤 4 hotel_reservation 此 API 用于预订请求的房间。 - 必选
- 步骤 5 booking_detail 此 API 用于获取特定预订的详细信息。  - 可选
- 步骤 6 get_cancellation_charges 此 API 用于获取特定预订在该时间点的取消费用。   - 必选
- 步骤 7 cancel_the_booking 此 API 用于取消特定预订。  - 必选
--- 
酒店搜索接口说明：

```text
http://colosseum.otrams.com:8087/ws/index.php?action=hotel_search&username=Heytrip_Test&password=Welcome@@123&checkin_date=22/04/2020&checkout_date=23/04/2020&sel_country=138&sel_city=71649&ch 
k_ratings=1.0,2.0,3.0,4.0,5.0&sel_nationality=106&country_of_residence=106&sel_currency=INR&availableonly=1&number_of_rooms=1&roomDetail 
s=[{"numberOfAdults":1}]&sel_hotel=&gzip=no&timeout=30&static_data=1&limit_hotel_room_type=5 
```

参数说明：
```text


action - 接口名称 示例：hotel_search
username - 用户名 示例：Heytrip_Test
password - 密码 示例：Welcome@@123

checkin_date - 入住日期，格式DD/MM/YYYY 示例：22/04/2020
checkout_date - 退房日期，格式DD/MM/YYYY 示例：23/04/202
sel_country - 国家ID 示例：138
sel_city - 城市ID 示例：71649
chk_ratings - 酒店星级，多个星级用逗号分隔 示例：1.0,2.0,3.0,4.0,5.0
sel_nationality - 国籍ID 示例：106
country_of_residence - 居住国家ID 示例：106
sel_currency - 货币代码 示例：INR
availableonly - 是否仅显示有房酒店 1-是 0-否 示例：1
sel_hotel - 酒店ID，多个ID用逗号分隔 示例：
gzip - 是否启用gzip压缩 1-是 0-否 示例：no/yes
static_data - 是否返回静态数据 1-是 0-否 示例：1
limit_hotel_room_type - 每个酒店返回的房型数量限制 示例：5
timeout - 请求超时时间，单位秒 示例：30

number_of_rooms - 房间数量 示例：1
roomDetails - 房间详细信息，JSON格式，数组形式，数组长度与number_of_rooms一致
房间详细信息示例： [{"numberOfAdults":2,"numberOfChild":1,"ChildAge":"5"}]
- numberOfAdults - 成人数量 示例：2
- numberOfChild - 儿童数量 示例：1
- ChildAge - 儿童年龄，多个年龄用逗号分隔 示例：5

```

酒店详情接口说明：

```text
http://colosseum.otrams.com/ws/index.php?action=hotel_detail&username=Heytrip_Test&password=Welcome@@123&hotel_id=OT000016097&unique_id=824-010-20250909112150-010-981795-010-1757416910823714670-010-
```
参数说明：
```text
action - 接口名称 示例：hotel_detail
username - 用户名 示例：Heytrip_Test
password - 密码 示例：Welcome@@123
hotel_id - 酒店ID 示例：OT000016097
unique_id - 酒店搜索接口返回的请求ID，来自hotel_search接口的响应 示例：824-010-20250909112150-010-981795-010-175741691082371
```


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

### 静态数据说明
• 测试环境和实时环境的静态数据文件不同。

• 静态数据文件将按日期生成在日期文件夹中。

• 将与测试访问一起通过电子邮件共享FTP。

• 要访问FTP，应在我们端将访问FTP的IP列入白名单。

• 静态数据应每15天或每月更新一次。


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