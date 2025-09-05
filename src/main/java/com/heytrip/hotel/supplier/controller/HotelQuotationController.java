package com.heytrip.hotel.supplier.controller;

import com.heytrip.hotel.supplier.dto.response.HotelSearchResponse;
import com.heytrip.hotel.supplier.service.HotelSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

/**
 * 报价类控制器
 * 提供酒店搜索相关的API接口
 * 
 * @author  Pax
 */
@RestController
@Validated
@RequestMapping("/quota")
public class HotelQuotationController {
    
    private static final Logger logger = LoggerFactory.getLogger(HotelQuotationController.class);
    
    @Autowired
    private HotelSearchService hotelSearchService;
    

    

    
    /**
     * 按价格范围过滤酒店搜索结果
     * POST /pax/api/xiwanSupplier/supp/hotels/filter/price
     */
    @PostMapping("/hotels/filter/price")
    public Mono<ResponseEntity<HotelSearchResponse>> filterByPrice(
            @RequestBody HotelSearchResponse searchResponse,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice) {
        
        logger.info("Filtering hotels by price range: {} - {}", minPrice, maxPrice);
        
        return Mono.fromCallable(() -> 
                hotelSearchService.filterByPriceRange(searchResponse, minPrice, maxPrice))
                .map(ResponseEntity::ok)
                .onErrorResume(error -> {
                    logger.error("Failed to filter hotels by price", error);
                    return Mono.just(ResponseEntity.badRequest().build());
                });
    }
    
    /**
     * 按星级过滤酒店搜索结果
     * POST /pax/api/xiwanSupplier/supp/hotels/filter/stars
     */
    @PostMapping("/hotels/filter/stars")
    public Mono<ResponseEntity<HotelSearchResponse>> filterByStars(
            @RequestBody HotelSearchResponse searchResponse,
            @RequestParam(required = false) BigDecimal minStars,
            @RequestParam(required = false) BigDecimal maxStars) {
        
        logger.info("Filtering hotels by star rating: {} - {}", minStars, maxStars);
        
        return Mono.fromCallable(() -> 
                hotelSearchService.filterByStarRating(searchResponse, minStars, maxStars))
                .map(ResponseEntity::ok)
                .onErrorResume(error -> {
                    logger.error("Failed to filter hotels by star rating", error);
                    return Mono.just(ResponseEntity.badRequest().build());
                });
    }
    
    /**
     * 按价格排序酒店搜索结果
     * POST /pax/api/xiwanSupplier/supp/hotels/sort/price
     */
    @PostMapping("/hotels/sort/price")
    public Mono<ResponseEntity<HotelSearchResponse>> sortByPrice(
            @RequestBody HotelSearchResponse searchResponse,
            @RequestParam(defaultValue = "true") boolean ascending) {
        
        logger.info("Sorting hotels by price, ascending: {}", ascending);
        
        return Mono.fromCallable(() -> 
                hotelSearchService.sortByPrice(searchResponse, ascending))
                .map(ResponseEntity::ok)
                .onErrorResume(error -> {
                    logger.error("Failed to sort hotels by price", error);
                    return Mono.just(ResponseEntity.badRequest().build());
                });
    }
    
    /**
     * 按星级排序酒店搜索结果
     * POST /pax/api/xiwanSupplier/supp/hotels/sort/stars
     */
    @PostMapping("/hotels/sort/stars")
    public Mono<ResponseEntity<HotelSearchResponse>> sortByStars(
            @RequestBody HotelSearchResponse searchResponse,
            @RequestParam(defaultValue = "false") boolean ascending) {
        
        logger.info("Sorting hotels by star rating, ascending: {}", ascending);
        
        return Mono.fromCallable(() -> 
                hotelSearchService.sortByStarRating(searchResponse, ascending))
                .map(ResponseEntity::ok)
                .onErrorResume(error -> {
                    logger.error("Failed to sort hotels by star rating", error);
                    return Mono.just(ResponseEntity.badRequest().build());
                });
    }
}
