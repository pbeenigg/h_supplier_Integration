package com.heytrip.hotel.supplier.adapter.service;

import com.heytrip.common.response.base.XHotel;
import com.heytrip.common.response.base.XRatePlan;
import com.heytrip.common.response.base.XRoom;
import com.heytrip.common.response.other.XCityResponse;
import com.heytrip.common.response.other.XCountryResponse;
import com.heytrip.hotel.supplier.dto.basic.XHotelGiata;
import com.heytrip.hotel.supplier.dto.basic.XNationality;
import com.heytrip.hotel.supplier.entity.SyncLog;
import org.springframework.data.domain.Page;
import java.util.List;
import java.util.Optional;

/**
 * 静态数据查询服务（分页+条件）
 * 仅先支持 AsianOverland 供应商
 */
public interface StaticDataQueryService {

    // ================= 分页查询（page*） =================
    Page<XCountryResponse> pageCountries(Long supplierId, String supplierCode, String countryCode, String countryName, int page, int size);

    Page<XCityResponse> pageCities(Long supplierId, String supplierCode, String cityCode, String countryCode, String name, int page, int size);

    Page<XHotel> pageHotels(Long supplierId, String supplierCode, String hotelCode, String cityCode, String countryCode, String name, int page, int size);

    Page<XRoom> pageRooms(Long supplierId, String supplierCode, String hotelCode, String roomCode, String name, int page, int size);

    Page<XRatePlan> pageRatePlans(Long supplierId, String supplierCode, String hotelCode, String roomCode, String ratePlanCode, String name, int page, int size);

    Page<XNationality> pageNationalities(Long supplierId, String supplierCode, String nationalityCode, String nationality, String isoCode, int page, int size);

    Page<XHotelGiata> pageGiataMappings(Long supplierId, String supplierCode, String hotelCode, String giataId, int page, int size);

    Page<SyncLog> pageSyncLogs(Long supplierId, String supplierCode, String businessType, Boolean success, int page, int size);



    // ================= 不分页查询（list*） =================
    List<XCountryResponse> listCountries(Long supplierId, String supplierCode, String countryCode, String countryName);

    List<XCityResponse> listCities(Long supplierId, String supplierCode, String cityCode, String countryCode, String name);

    List<XHotel> listHotels(Long supplierId, String supplierCode, String hotelCode, String cityCode, String countryCode, String name);

    List<XRoom> listRooms(Long supplierId, String supplierCode, String hotelCode, String roomCode, String name);

    List<XRatePlan> listRatePlans(Long supplierId, String supplierCode, String hotelCode, String roomCode, String ratePlanCode, String name);

    List<XNationality> listNationalities(Long supplierId, String supplierCode, String nationalityCode, String nationality, String isoCode);

    List<XHotelGiata> listGiataMappings(Long supplierId, String supplierCode, String hotelCode, String giataId);



    // ================= 单条查询（ByCode） =================
    Optional<XHotel> getHotelByHotelCode(Long supplierId, String supplierCode, String hotelCode);

    Optional<XRoom> getRoomByRoomCode(Long supplierId, String supplierCode, String hotelCode, String roomCode);

    Optional<XRatePlan> getRatePlanByRatePlanCode(Long supplierId, String supplierCode, String hotelCode, String roomCode, String ratePlanCode);

    Optional<XNationality> getNationalityByNationalityCode(Long supplierId, String supplierCode, String nationalityCode);

    Optional<XCountryResponse> getCountryByCountryCode(Long supplierId, String supplierCode, String countryCode);

    Optional<XCityResponse> getCityByCityCode(Long supplierId, String supplierCode, String cityCode);
}
