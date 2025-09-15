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
    Page<XCountryResponse> pageCountries(Long supplierId, String supplierName, String countryCode, String countryName, int page, int size);

    Page<XCityResponse> pageCities(Long supplierId, String supplierName, String cityCode, String countryCode, String name, int page, int size);

    Page<XHotel> pageHotels(Long supplierId, String supplierName, String hotelCode, String cityCode, String countryCode, String name, int page, int size);

    Page<XRoom> pageRooms(Long supplierId, String supplierName, String hotelCode, String roomCode, String name, int page, int size);

    Page<XRatePlan> pageRatePlans(Long supplierId, String supplierName, String hotelCode, String roomCode, String ratePlanCode, String name, int page, int size);

    Page<XNationality> pageNationalities(Long supplierId, String supplierName, String nationalityCode, String nationality, String isoCode, int page, int size);

    Page<XHotelGiata> pageGiataMappings(Long supplierId, String supplierName, String hotelCode, String giataId, int page, int size);

    Page<SyncLog> pageSyncLogs(Long supplierId, String supplierName, String businessType, Boolean success, int page, int size);



    // ================= 不分页查询（list*） =================
    List<XCountryResponse> listCountries(Long supplierId, String supplierName, String countryCode, String countryName);

    List<XCityResponse> listCities(Long supplierId, String supplierName, String cityCode, String countryCode, String name);

    List<XHotel> listHotels(Long supplierId, String supplierName, String hotelCode, String cityCode, String countryCode, String name);

    List<XRoom> listRooms(Long supplierId, String supplierName, String hotelCode, String roomCode, String name);

    List<XRatePlan> listRatePlans(Long supplierId, String supplierName, String hotelCode, String roomCode, String ratePlanCode, String name);

    List<XNationality> listNationalities(Long supplierId, String supplierName, String nationalityCode, String nationality, String isoCode);

    List<XHotelGiata> listGiataMappings(Long supplierId, String supplierName, String hotelCode, String giataId);



    // ================= 单条查询（ByCode） =================
    Optional<XHotel> getHotelByHotelCode(Long supplierId, String supplierName, String hotelCode);

    Optional<XRoom> getRoomByRoomCode(Long supplierId, String supplierName, String hotelCode, String roomCode);

    Optional<XRatePlan> getRatePlanByRatePlanCode(Long supplierId, String supplierName, String hotelCode, String roomCode, String ratePlanCode);

    Optional<XNationality> getNationalityByNationalityCode(Long supplierId, String supplierName, String nationalityCode);

    Optional<XCountryResponse> getCountryByCountryCode(Long supplierId, String supplierName, String countryCode);

    Optional<XCityResponse> getCityByCityCode(Long supplierId, String supplierName, String cityCode);
}
