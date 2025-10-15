package com.heytrip.hotel.supplier.adapter.service;

import com.heytrip.common.response.base.XHotel;
import com.heytrip.common.response.base.XRatePlan;
import com.heytrip.common.response.base.XRoom;
import com.heytrip.common.response.other.XCityResponse;
import com.heytrip.common.response.other.XCountryResponse;
import com.heytrip.hotel.supplier.dto.basic.XHotelGiata;
import com.heytrip.hotel.supplier.dto.basic.XNationality;
import com.heytrip.hotel.supplier.entity.Hotel;
import com.heytrip.hotel.supplier.entity.Room;
import com.heytrip.hotel.supplier.entity.SyncLog;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 静态数据查询服务（分页+条件）
 * 仅先支持 AsianOverland 供应商
 */
public interface StaticDataQueryService {

    // ================= 分页查询（page*） =================
    Page<XCountryResponse> pageCountries(Long supplierId, String supplierCode, String countryCode, int page, int size);
    Page<XCountryResponse> pageCountries(Long supplierId, String supplierCode, int page, int size);

    Page<XCityResponse> pageCities(Long supplierId, String supplierCode, String cityCode, String countryCode, int page, int size);
    Page<XCityResponse> pageCities(Long supplierId, String supplierCode, int page, int size);

    Page<XNationality> pageNationalities(Long supplierId, String supplierCode, String nationalityCode, String isoCode, int page, int size);
    Page<XNationality> pageNationalities(Long supplierId, String supplierCode, int page, int size);

    Page<XHotelGiata> pageGiataMappings(Long supplierId, String supplierCode, String hotelCode, String giataId, int page, int size);
    Page<XHotelGiata> pageGiataMappings(Long supplierId, String supplierCode, int page, int size);


    Page<XHotel> pageHotels(Long supplierId, String supplierCode, String cityCode, String countryCode, int page, int size);
    Page<XHotel> pageHotels(Long supplierId, String supplierCode, int page, int size);

    Page<XRoom> pageRooms(Long supplierId, String supplierCode, String hotelCode, String roomCode, int page, int size);
    Page<XRoom> pageRooms(Long supplierId, String supplierCode, int page, int size);

    Page<XRatePlan> pageRatePlans(Long supplierId, String supplierCode, String hotelCode, String roomCode, String ratePlanCode, int page, int size);



    Page<SyncLog> pageSyncLogs(Long supplierId, String supplierCode, String businessType, Boolean success, int page, int size);



    // ================= 不分页查询（list*） =================
    List<XCountryResponse> listCountries(Long supplierId, String supplierCode, String countryCode);

    List<XCityResponse> listCities(Long supplierId, String supplierCode, String cityCode, String countryCode);

    List<XHotel> listHotels(Long supplierId, String supplierCode, String hotelCode, String cityCode, String countryCode);

    List<XRoom> listRooms(Long supplierId, String supplierCode, String hotelCode, String roomCode);

    List<XRatePlan> listRatePlans(Long supplierId, String supplierCode, String hotelCode, String roomCode, String ratePlanCode);

    List<XNationality> listNationalities(Long supplierId, String supplierCode, String nationalityCode, String isoCode);

    List<XHotelGiata> listGiataMappings(Long supplierId, String supplierCode, String hotelCode, String giataId);



    // ================= 单条查询（ByCode） =================
    Optional<XHotel> getHotelByHotelCode(Long supplierId, String supplierCode, String hotelCode);

    Optional<XRoom> getRoomByRoomCode(Long supplierId, String supplierCode,String roomCode);

    Optional<XRatePlan> getRatePlanByRatePlanCode(Long supplierId, String supplierCode,String ratePlanCode);

    Optional<XNationality> getNationalityByNationalityCode(Long supplierId, String supplierCode, String nationalityCode);

    Optional<XCountryResponse> getCountryByCountryCode(Long supplierId, String supplierCode, String countryCode);

    Optional<XCityResponse> getCityByCityCode(Long supplierId, String supplierCode, String cityCode);

    // ================= 增量查询（基于自增ID） =================
    
    /**
     * 酒店基础信息增量查询
     * 基于自增ID的增量查询，查询ID大于指定maxId的酒店记录
     * 
     * @param supplierId 供应商ID
     * @param supplierCode 供应商代码
     * @param maxId 上次请求的最大增量编号（酒店ID，传0表示从头开始）
     * @param pageSize 每页大小（最大100）
     * @return 分页的酒店增量数据
     */
    Page<Hotel> getIncrementalHotels(Long supplierId, String supplierCode, Long maxId, int pageSize);
    
    /**
     * 酒店基础信息增量查询（带时间过滤）
     * 基于自增ID和更新时间的增量查询
     * 
     * @param supplierId 供应商ID
     * @param supplierCode 供应商代码
     * @param maxId 上次请求的最大增量编号（酒店ID，传0表示从头开始）
     * @param minTime 最小更新时间
     * @param pageSize 每页大小（最大100）
     * @return 分页的酒店增量数据
     */
    Page<Hotel> getIncrementalHotelsByTime(Long supplierId, String supplierCode, Long maxId, LocalDateTime minTime, int pageSize);
    
    /**
     * 房型基础信息增量查询
     * 基于自增ID的增量查询，查询ID大于指定maxId的房型记录
     * 
     * @param supplierId 供应商ID
     * @param supplierCode 供应商代码
     * @param maxId 上次请求的最大增量编号（房型ID，传0表示从头开始）
     * @param pageSize 每页大小（最大100）
     * @return 分页的房型增量数据
     */
    Page<Room> getIncrementalRooms(Long supplierId, String supplierCode, Long maxId, int pageSize);
    
    /**
     * 房型基础信息增量查询（带时间过滤）
     * 基于自增ID和更新时间的增量查询
     * 
     * @param supplierId 供应商ID
     * @param supplierCode 供应商代码
     * @param maxId 上次请求的最大增量编号（房型ID，传0表示从头开始）
     * @param minTime 最小更新时间
     * @param pageSize 每页大小（最大100）
     * @return 分页的房型增量数据
     */
    Page<Room> getIncrementalRoomsByTime(Long supplierId, String supplierCode, Long maxId, LocalDateTime minTime, int pageSize);
    

}
