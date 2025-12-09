package com.heytrip.hotel.supplier.adapter.parser;

import cn.hutool.core.util.StrUtil;
import com.heytrip.hotel.supplier.entity.*;
import com.heytrip.hotel.supplier.utils.CoordinateUtil;
import com.heytrip.hotel.supplier.utils.MD5Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * AsianOverland 静态数据解析策略实现
 * 说明：
 * - 所有 CSV 均有 header，按列名映射
 * - 主键缺失的行跳过，由上层统计 skipCount
 */
@Component
public class AOStaticDataParser implements StaticDataParser {

    private static final Logger logger = LoggerFactory.getLogger(AOStaticDataParser.class);

    @Override
    public List<Country> parseCountries(List<Map<String, String>> rows, Long supplierId, String supplierCode,Map<String ,String> isoMap) {
        List<Country> list = new ArrayList<>();
        for (Map<String, String> row : rows) {
            String countryCode = val(row, "country_code");
            if (isBlank(countryCode)) {
                countryCode = val(row, "code");
                if (isBlank(countryCode)) {
                    continue;
                }
            }

            Country e = new Country();
            e.setSupplierId(supplierId);
            e.setSupplierCode(supplierCode);
            e.setCountryId(countryCode);
            e.setCountryName(val(row, "country_name"));

            if(isoMap != null && isoMap.containsKey(countryCode)){
                e.setCountryCode(isoMap.get(countryCode));
            }

            list.add(e);
        }
        return list;
    }

    @Override
    public List<City> parseCities(List<Map<String, String>> rows, Long supplierId, String supplierCode) {
        List<City> list = new ArrayList<>();
        for (Map<String, String> row : rows) {
            String cityCode = val(row, "city_code");
            if (isBlank(cityCode)) {
                continue;
            }
            City e = new City();
            e.setSupplierId(supplierId);
            e.setSupplierCode(supplierCode);
            e.setCityCode(cityCode);
            e.setName(val(row, "name"));
            e.setCountryCode(val(row, "country_code"));
            e.setCountryName(val(row, "country_name"));
            list.add(e);
        }
        return list;
    }

    @Override
    public List<Hotel> parseHotels(List<Map<String, String>> rows, Long supplierId, String supplierCode, Map<String, Country> countryMap) {


        List<Hotel> list = new ArrayList<>();
        for (Map<String, String> row : rows) {
            String hotelCode = val(row, "Id");
            if (isBlank(hotelCode)) {
                hotelCode = val(row, "system_id");
                if (isBlank(hotelCode)) {
                    continue;
                }
            }
            Hotel e = new Hotel();
            e.setSupplierId(supplierId);
            e.setSupplierCode(supplierCode);
            e.setHotelCode(hotelCode);
            // 当供应商酒店ID超过64字符，使用原始ID的SHA-256（64位十六进制）作为 hotelCodeMd5；否则直接使用原始ID
            String hotelCodeMd5 = hotelCode.length() > 32 ? MD5Util.string2MD5(hotelCode) : hotelCode;
            e.setHotelCodeMd5(hotelCodeMd5);
            e.setHotelName(val(row, "NAME"));
            e.setCityCode(val(row, "city_code"));
            e.setCity(val(row, "city_name"));
            
            // 设置country_id，限制长度为100字符
            String countryIdStr = val(row, "country_code");
            if(StrUtil.isNotBlank(countryIdStr)){
                if(countryIdStr.length() > 100){
                    logger.warn("国家编号超长({}字符)，已截断: {}, 酒店编号: {}", 
                        countryIdStr.length(), countryIdStr.substring(0, 50) + "...", hotelCode);
                    countryIdStr = countryIdStr.substring(0, 100);
                }
                e.setCountryId(countryIdStr);
            }

            // 补充国家名称和代码
            if(StrUtil.isNotBlank(e.getCountryId()) && countryMap != null && countryMap.containsKey(e.getCountryId())){
                Country country = countryMap.get(e.getCountryId());
                if(country != null){
                    e.setCountryCode(country.getCountryCode());
                    e.setCountry(country.getCountryName());
                }
            }

            e.setHeroImg(val(row, "main_image"));
            e.setDescription(val(row, "short_desc"));
            e.setLongDesc(val(row, "long_desc"));
            e.setAddress(val(row, "address"));
            e.setPhone(val(row, "phone"));
            e.setWebsite(val(row, "website"));


            ///酒店经纬度解析，处理异常情况， 只保留正确的经纬度数据，如果所有的解析手段都处理不了，则设置为null
            //1、经度纬度字段可能为空或格式不正确
            //2、经度纬度字段可能超出合理范围
            //3、经度纬度字段可能为0
            //4、经度纬度字段可能存在小数点后过多位数的情况
            //5、经度纬度字段可能存在负数的情况
            //6、经度纬度字段可能存在科学计数法表示的情况
            //7、经度纬度字段可能存在前后空格的情况
            //8、经度纬度字段可能存在中文符号的情况
            //9、经度纬度字段可能存在特殊符号的情况
            //10、经度纬度字段可能存在多余的字符情况
            ///举例：
            // 3.162790, 101.711120,17   -> 有逗号 -> 3.162790,101.711120
            // 3.0848&deg; N , 101.6733&deg; E  -> 有分号字母    和特殊符号 -> 3.0848 , 101.6733
            // 3.0817076 ,101.5599172,   -> 有多余逗号 -> 3.0817076 ,101.5599172
            e.setLatitude(CoordinateUtil.parseCoordinate(val(row, "latitude"), "纬度", -90.0, 90.0, hotelCode));
            e.setLongitude(CoordinateUtil.parseCoordinate(val(row, "longitude"), "经度", -180.0, 180.0, hotelCode));
            e.setRating(val(row, "rating"));
            list.add(e);
        }
        return list;
    }

    @Override
    public List<Nationality> parseNationalities(List<Map<String, String>> rows, Long supplierId, String supplierCode) {
        List<Nationality> list = new ArrayList<>();
        for (Map<String, String> row : rows) {
            String nationalityCode = val(row, "CODE");
            if (isBlank(nationalityCode)) {
                continue;
            }
            Nationality e = new Nationality();
            e.setSupplierId(supplierId);
            e.setSupplierCode(supplierCode);
            e.setNationalityCode(nationalityCode);
            e.setNationality(val(row, "nationality"));
            e.setIsoCode(val(row, "iso_code"));
            list.add(e);
        }
        return list;
    }

    @Override
    public List<HotelGiata> parseGiataMappings(List<Map<String, String>> rows, Long supplierId, String supplierCode) {
        List<HotelGiata> list = new ArrayList<>();
        for (Map<String, String> row : rows) {
            String hotelCode = val(row, "Id");
            String giataId = val(row, "giata_id");
            if (isBlank(hotelCode) || isBlank(giataId)) {
                continue;
            }
            HotelGiata e = new HotelGiata();
            e.setSupplierId(supplierId);
            e.setSupplierCode(supplierCode);
            e.setHotelCode(hotelCode);
            e.setGiataId(giataId);
            e.setName(val(row, "NAME"));
            e.setCityCode(val(row, "city_code"));
            e.setCityName(val(row, "city_name"));
            e.setCountryCode(val(row, "country_code"));
            e.setLongDesc(val(row, "long_desc"));
            e.setLatitude(parseDouble(val(row, "latitude")));
            e.setLongitude(parseDouble(val(row, "longitude")));
            e.setRating(parseDouble(val(row, "rating")));
            e.setAddress(val(row, "address"));
            e.setMainImage(val(row, "main_image"));
            list.add(e);
        }
        return list;
    }

    @Override
    public List<Room> parseRooms(List<Map<String, String>> rows, Long supplierId, String supplierCode) {
        // 当前无房型静态文件，预留
        return new ArrayList<>();
    }

    @Override
    public List<RatePlan> parseRatePlans(List<Map<String, String>> rows, Long supplierId, String supplierCode) {
        // 当前无价计划静态文件，预留
        return new ArrayList<>();
    }

    private String val(Map<String, String> row, String key) {
        if (row == null) return null;
        String v = row.get(key);
        return v == null ? null : v.trim();
    }

    private boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }

    private Double parseDouble(String s) {
        try {
            if (isBlank(s)) return null;
            return Double.parseDouble(s.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private BigDecimal parseBigDecimal(String s) {
        try {
            if (isBlank(s)) return BigDecimal.ZERO;
            return new BigDecimal(s.trim());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }



}
