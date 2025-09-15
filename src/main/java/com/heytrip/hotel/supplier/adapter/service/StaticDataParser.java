package com.heytrip.hotel.supplier.adapter.service;

import com.heytrip.hotel.supplier.entity.*;

import java.util.List;
import java.util.Map;

/**
 * 静态数据解析接口（按供应商实现具体策略）
 * 约定输入：一行CSV转为的列名->值的Map
 * 约定输出：对应的实体列表（由调用方批量入库）
 */
public interface StaticDataParser {

    List<Country> parseCountries(List<Map<String, String>> rows, Long supplierId, String supplierCode);

    List<City> parseCities(List<Map<String, String>> rows, Long supplierId, String supplierCode);

    List<Hotel> parseHotels(List<Map<String, String>> rows, Long supplierId, String supplierCode);

    List<Nationality> parseNationalities(List<Map<String, String>> rows, Long supplierId, String supplierCode);

    List<HotelGiata> parseGiataMappings(List<Map<String, String>> rows, Long supplierId, String supplierCode);

    // 预留：房型、价计划（静态维度），如后续文件提供
    List<Room> parseRooms(List<Map<String, String>> rows, Long supplierId, String supplierCode);

    List<RatePlan> parseRatePlans(List<Map<String, String>> rows, Long supplierId, String supplierCode);
}
