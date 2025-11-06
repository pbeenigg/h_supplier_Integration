package com.heytrip.hotel.supplier.adapter.parser;

import cn.hutool.core.util.StrUtil;
import com.heytrip.hotel.supplier.entity.*;
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
    public List<Country> parseCountries(List<Map<String, String>> rows, Long supplierId, String supplierCode) {
        List<Country> list = new ArrayList<>();
        for (Map<String, String> row : rows) {
            String countryCode = val(row, "country_code");
            if (isBlank(countryCode)) {
                continue;
            }
            Country e = new Country();
            e.setSupplierId(supplierId);
            e.setSupplierCode(supplierCode);
            e.setCountryId(countryCode);
            e.setCountryName(val(row, "country_name"));
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
    public List<Hotel> parseHotels(List<Map<String, String>> rows, Long supplierId, String supplierCode, Map<Long, Country> countryMap) {


        List<Hotel> list = new ArrayList<>();
        for (Map<String, String> row : rows) {
            String hotelCode = val(row, "Id");
            if (isBlank(hotelCode)) {
                continue;
            }
            Hotel e = new Hotel();
            e.setSupplierId(supplierId);
            e.setSupplierCode(supplierCode);
            e.setHotelCode(hotelCode);
            // 当供应商酒店ID超过64字符，使用原始ID的SHA-256（64位十六进制）作为 hotelCodeMd5；否则直接使用原始ID
            String hotelCodeMd5 = hotelCode.length() > 64 ? MD5Util.string2MD5(hotelCode) : hotelCode;
            e.setHotelCodeMd5(hotelCodeMd5);
            e.setHotelName(val(row, "NAME"));
            e.setCityCode(val(row, "city_code"));
            e.setCity(val(row, "city_name"));
            e.setCountryId(val(row, "country_code"));

            // 补充国家名称和代码
            if(StrUtil.isNotBlank(e.getCountryId())){
                Country country = countryMap.getOrDefault(Long.parseLong(e.getCountryId()),null);
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
            e.setLatitude(val(row, "latitude"));
            e.setLongitude(val(row, "longitude"));
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
