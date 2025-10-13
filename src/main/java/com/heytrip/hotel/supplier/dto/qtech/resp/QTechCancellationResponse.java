package com.heytrip.hotel.supplier.dto.qtech.resp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * QTECH取消预订响应DTO
 * 
 * @author Pax
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class QTechCancellationResponse  extends QTechBaseResponse{
    

    
    // 木有字段
}
