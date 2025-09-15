package com.heytrip.hotel.supplier.adapter.service;

import com.heytrip.common.response.base.XHotel;
import com.heytrip.common.response.base.XRatePlan;
import com.heytrip.common.response.base.XRoom;
import com.heytrip.common.response.other.XCityResponse;
import com.heytrip.common.response.other.XCountryResponse;
import com.heytrip.hotel.supplier.dto.basic.XNationality;
import com.heytrip.hotel.supplier.dto.basic.XHotelGiata;
import com.heytrip.hotel.supplier.entity.SyncLog;
import org.springframework.data.domain.Page;

/**
 * 静态数据查询服务（分页+条件）
 * 仅先支持 AsianOverland 供应商
 */
public interface StaticDataQueryService {

    Page<XCountryResponse> pageCountries(Long supplierId, String supplierCode, String countryCode, String countryName, int page, int size);

    Page<XCityResponse> pageCities(Long supplierId, String supplierCode, String cityCode, String countryCode, String name, int page, int size);

    Page<XHotel> pageHotels(Long supplierId, String supplierCode, String hotelCode, String cityCode, String countryCode, String name, int page, int size);

    Page<XRoom> pageRooms(Long supplierId, String supplierCode, String hotelCode, String roomCode, String name, int page, int size);

    Page<XRatePlan> pageRatePlans(Long supplierId, String supplierCode, String hotelCode, String roomCode, String ratePlanCode, String name, int page, int size);

    Page<XNationality> pageNationalities(Long supplierId, String supplierCode, String nationalityCode, String nationality, String isoCode, int page, int size);

    Page<XHotelGiata> pageGiataMappings(Long supplierId, String supplierCode, String hotelCode, String giataId, int page, int size);

    /**
     * 静态数据同步日志分页查询
     * @param supplierId 供应商ID
     * @param supplierCode 供应商代码
     * @param businessType 业务类型（countries/cities/hotels/nationality/giata/all）可选
     * @param success 是否成功 可选
     */
    Page<SyncLog> pageStaticSyncLogs(Long supplierId, String supplierCode, String businessType, Boolean success, int page, int size);
}
