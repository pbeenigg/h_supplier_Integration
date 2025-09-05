package com.heytrip.hotel.supplier.service;

import com.heytrip.hotel.supplier.adapter.SupplierAdapterManager;
import com.heytrip.hotel.supplier.dto.request.HotelSearchRequest;
import com.heytrip.hotel.supplier.dto.response.HotelSearchResponse;
import com.heytrip.hotel.supplier.dto.response.HotelInfo;
import com.heytrip.hotel.supplier.entity.Hotel;
import com.heytrip.hotel.supplier.repository.HotelRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 酒店搜索服务
 * 负责酒店搜索业务逻辑，包括缓存、过滤、排序等
 * 
 * @author  Pax
 */
@Service
public class HotelSearchService {
    
    private static final Logger logger = LoggerFactory.getLogger(HotelSearchService.class);
    
    @Autowired
    private SupplierAdapterManager supplierAdapterManager;
    
    @Autowired
    private HotelRepository hotelRepository;
    
    @Autowired
    private CacheService cacheService;
    
    /**
     * 搜索酒店 - 聚合所有供应商结果
     * @param request 搜索请求
     * @return 搜索结果
     */
    @Cacheable(value = "hotelSearch", key = "#request.city + '_' + #request.checkInDate + '_' + #request.checkOutDate + '_' + #request.roomCount + '_' + #request.guestCount")
    public Mono<HotelSearchResponse> searchHotels(HotelSearchRequest request) {
        logger.info("Starting hotel search for city: {}, dates: {} to {}", 
                request.getCity(), request.getCheckInDate(), request.getCheckOutDate());
        
        // 验证请求参数
        validateSearchRequest(request);
        
        // 从供应商聚合搜索
        return supplierAdapterManager.searchHotelsFromAllSuppliers(request)
                .map(response -> enhanceSearchResponse(response, request))
                .doOnSuccess(response -> 
                        logger.info("Hotel search completed for city: {}, found {} hotels", 
                                request.getCity(), response.getTotalCount()))
                .doOnError(error -> 
                        logger.error("Hotel search failed for city: {}", request.getCity(), error));
    }
    
    /**
     * 从指定供应商搜索酒店
     * @param supplierName 供应商名称
     * @param request 搜索请求
     * @return 搜索结果
     */
    public Mono<HotelSearchResponse> searchHotelsFromSupplier(String supplierName, HotelSearchRequest request) {
        logger.info("Searching hotels from supplier: {} for city: {}", supplierName, request.getCity());
        
        validateSearchRequest(request);
        
        return supplierAdapterManager.searchHotelsFromSupplier(supplierName, request)
                .map(response -> enhanceSearchResponse(response, request))
                .doOnSuccess(response -> 
                        logger.info("Hotel search from supplier {} completed, found {} hotels", 
                                supplierName, response.getTotalCount()));
    }
    
    /**
     * 获取热门城市的酒店
     * @param city 城市名称
     * @param limit 限制数量
     * @return 酒店列表
     */
    @Cacheable(value = "popularHotels", key = "#city + '_' + #limit")
    public Mono<List<HotelInfo>> getPopularHotels(String city, int limit) {
        logger.info("Getting popular hotels for city: {}, limit: {}", city, limit);
        
        return Mono.fromCallable(() -> {
            List<Hotel> hotels = hotelRepository.findActiveHotelsByCity(city);
            return hotels.stream()
                    .limit(limit)
                    .map(this::convertToHotelInfo)
                    .collect(Collectors.toList());
        });
    }
    
    /**
     * 根据价格范围过滤酒店
     * @param response 原始搜索结果
     * @param minPrice 最低价格
     * @param maxPrice 最高价格
     * @return 过滤后的结果
     */
    public HotelSearchResponse filterByPriceRange(HotelSearchResponse response, 
                                                 BigDecimal minPrice, BigDecimal maxPrice) {
        if (response.getHotels() == null) {
            return response;
        }
        
        List<HotelInfo> filteredHotels = response.getHotels().stream()
                .filter(hotel -> {
                    BigDecimal price = hotel.getLowestPrice();
                    if (price == null) return false;
                    
                    boolean withinRange = true;
                    if (minPrice != null) {
                        withinRange = price.compareTo(minPrice) >= 0;
                    }
                    if (maxPrice != null && withinRange) {
                        withinRange = price.compareTo(maxPrice) <= 0;
                    }
                    return withinRange;
                })
                .collect(Collectors.toList());
        
        HotelSearchResponse filteredResponse = new HotelSearchResponse();
        filteredResponse.setHotels(filteredHotels);
        filteredResponse.setTotalCount(filteredHotels.size());
        filteredResponse.setCurrency(response.getCurrency());
        filteredResponse.setMessage("Filtered by price range");
        
        return filteredResponse;
    }
    
    /**
     * 根据星级过滤酒店
     * @param response 原始搜索结果
     * @param minStars 最低星级
     * @param maxStars 最高星级
     * @return 过滤后的结果
     */
    public HotelSearchResponse filterByStarRating(HotelSearchResponse response, 
                                                 BigDecimal minStars, BigDecimal maxStars) {
        if (response.getHotels() == null) {
            return response;
        }
        
        List<HotelInfo> filteredHotels = response.getHotels().stream()
                .filter(hotel -> {
                    BigDecimal stars = hotel.getStarRating();
                    if (stars == null) return false;
                    
                    boolean withinRange = true;
                    if (minStars != null) {
                        withinRange = stars.compareTo(minStars) >= 0;
                    }
                    if (maxStars != null && withinRange) {
                        withinRange = stars.compareTo(maxStars) <= 0;
                    }
                    return withinRange;
                })
                .collect(Collectors.toList());
        
        HotelSearchResponse filteredResponse = new HotelSearchResponse();
        filteredResponse.setHotels(filteredHotels);
        filteredResponse.setTotalCount(filteredHotels.size());
        filteredResponse.setCurrency(response.getCurrency());
        filteredResponse.setMessage("Filtered by star rating");
        
        return filteredResponse;
    }
    
    /**
     * 按价格排序酒店
     * @param response 原始搜索结果
     * @param ascending 是否升序
     * @return 排序后的结果
     */
    public HotelSearchResponse sortByPrice(HotelSearchResponse response, boolean ascending) {
        if (response.getHotels() == null) {
            return response;
        }
        
        List<HotelInfo> sortedHotels = response.getHotels().stream()
                .sorted((h1, h2) -> {
                    BigDecimal price1 = h1.getLowestPrice();
                    BigDecimal price2 = h2.getLowestPrice();
                    
                    if (price1 == null && price2 == null) return 0;
                    if (price1 == null) return 1;
                    if (price2 == null) return -1;
                    
                    int comparison = price1.compareTo(price2);
                    return ascending ? comparison : -comparison;
                })
                .collect(Collectors.toList());
        
        HotelSearchResponse sortedResponse = new HotelSearchResponse();
        sortedResponse.setHotels(sortedHotels);
        sortedResponse.setTotalCount(sortedHotels.size());
        sortedResponse.setCurrency(response.getCurrency());
        sortedResponse.setMessage("Sorted by price " + (ascending ? "ascending" : "descending"));
        
        return sortedResponse;
    }
    
    /**
     * 按星级排序酒店
     * @param response 原始搜索结果
     * @param ascending 是否升序
     * @return 排序后的结果
     */
    public HotelSearchResponse sortByStarRating(HotelSearchResponse response, boolean ascending) {
        if (response.getHotels() == null) {
            return response;
        }
        
        List<HotelInfo> sortedHotels = response.getHotels().stream()
                .sorted((h1, h2) -> {
                    BigDecimal stars1 = h1.getStarRating();
                    BigDecimal stars2 = h2.getStarRating();
                    
                    if (stars1 == null && stars2 == null) return 0;
                    if (stars1 == null) return 1;
                    if (stars2 == null) return -1;
                    
                    int comparison = stars1.compareTo(stars2);
                    return ascending ? comparison : -comparison;
                })
                .collect(Collectors.toList());
        
        HotelSearchResponse sortedResponse = new HotelSearchResponse();
        sortedResponse.setHotels(sortedHotels);
        sortedResponse.setTotalCount(sortedHotels.size());
        sortedResponse.setCurrency(response.getCurrency());
        sortedResponse.setMessage("Sorted by star rating " + (ascending ? "ascending" : "descending"));
        
        return sortedResponse;
    }
    
    /**
     * 增强搜索响应
     */
    private HotelSearchResponse enhanceSearchResponse(HotelSearchResponse response, HotelSearchRequest request) {
        if (response.getHotels() != null) {
            // 计算住宿天数
            long nights = ChronoUnit.DAYS.between(request.getCheckInDate(), request.getCheckOutDate());
            
            // 为每个酒店添加额外信息
            response.getHotels().forEach(hotel -> {
                // 设置搜索时间
                response.setSearchTime(LocalDateTime.now());
                
                // 计算总价（如果有每晚价格）
                if (hotel.getLowestPrice() != null && nights > 0) {
                    BigDecimal totalPrice = hotel.getLowestPrice().multiply(BigDecimal.valueOf(nights));
                    // 可以添加到酒店信息中，这里暂时不修改DTO结构
                }
            });
        }
        
        return response;
    }
    
    /**
     * 验证搜索请求
     */
    private void validateSearchRequest(HotelSearchRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Search request cannot be null");
        }
        
        if (request.getCity() == null || request.getCity().trim().isEmpty()) {
            throw new IllegalArgumentException("City is required");
        }
        
        if (request.getCheckInDate() == null) {
            throw new IllegalArgumentException("Check-in date is required");
        }
        
        if (request.getCheckOutDate() == null) {
            throw new IllegalArgumentException("Check-out date is required");
        }
        
        if (request.getCheckInDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Check-in date cannot be in the past");
        }
        
        if (request.getCheckInDate().isAfter(request.getCheckOutDate()) || 
            request.getCheckInDate().isEqual(request.getCheckOutDate())) {
            throw new IllegalArgumentException("Check-out date must be after check-in date");
        }
        
        if (request.getRoomCount() == null || request.getRoomCount() <= 0) {
            throw new IllegalArgumentException("Room count must be greater than 0");
        }
        
        if (request.getGuestCount() == null || request.getGuestCount() <= 0) {
            throw new IllegalArgumentException("Guest count must be greater than 0");
        }
        
        // 检查日期范围是否合理（不超过1年）
        long daysBetween = ChronoUnit.DAYS.between(request.getCheckInDate(), request.getCheckOutDate());
        if (daysBetween > 365) {
            throw new IllegalArgumentException("Stay duration cannot exceed 365 days");
        }
    }
    
    /**
     * 将Hotel实体转换为HotelInfo DTO
     */
    private HotelInfo convertToHotelInfo(Hotel hotel) {
        HotelInfo hotelInfo = new HotelInfo();
        hotelInfo.setHotelId(hotel.getId().toString());
        hotelInfo.setSupplierHotelId(hotel.getSupplierHotelId());
        hotelInfo.setHotelName(hotel.getHotelName());
        hotelInfo.setHotelAddress(hotel.getHotelAddress());
        hotelInfo.setCity(hotel.getCity());
        hotelInfo.setCountry(hotel.getCountry());
        hotelInfo.setStarRating(hotel.getStarRating());
        hotelInfo.setLatitude(hotel.getLatitude());
        hotelInfo.setLongitude(hotel.getLongitude());
        hotelInfo.setDescription(hotel.getDescription());
        hotelInfo.setIsActive(hotel.getIsActive());
        
        // 解析amenities和images（假设存储为JSON字符串）
        if (hotel.getAmenities() != null) {
            // 简单分割，实际项目中应使用JSON解析
            hotelInfo.setAmenities(List.of(hotel.getAmenities().split(",")));
        }
        
        if (hotel.getImages() != null) {
            hotelInfo.setImages(List.of(hotel.getImages().split(",")));
        }
        
        return hotelInfo;
    }
}
