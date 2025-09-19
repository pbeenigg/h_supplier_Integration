package com.heytrip.hotel.supplier.enums;

/**
 * QTECH预订状态枚举
 * 
 * 定义了QTECH供应商的所有预订状态及其描述
 * 
 * @author Pax
 */
public enum QTechBookingStatusEnum {

    /**
     * 预订已在系统中正确发布并确认
     */
    VOUCHERED("vouchered", "预订已在系统中正确发布并确认"),
    
    /**
     * 预订尚未确认，后续可能确认或被拒；建议使用 booking_detail 定时轮询，直到确认或拒绝
     */
    ON_REQUEST("on_request", "预订尚未确认，后续可能确认或被拒；建议使用 booking_detail 定时轮询，直到确认或拒绝"),
    
    /**
     * 预订未确认或已被取消
     */
    REJECTED("rejected", "预订未确认或已被取消"),
    
    /**
     * 因第三方错误或请求技术错误导致预订失败
     */
    FAILED("failed", "因第三方错误或请求技术错误导致预订失败"),
    
    /**
     * 因网络/服务器问题未正确入库。遇到此状态需直接联系 Qtech 客服确认处理
     */
    INPROCESS_BOOKING("inprocess-booking", "因网络/服务器问题未正确入库。遇到此状态需直接联系 Qtech 客服确认处理"),
    
    /**
     * 当你在响应中收到一个错误"Response Cancel"，并且你通过预订详情API检查此状态时，你会在预订详情响应中收到状态为inprocess_cancel
     */
    INPROCESS_CANCEL("inprocess_cancel", "当你在响应中收到一个错误\"Response Cancel\"，并且你通过预订详情API检查此状态时，你会在预订详情响应中收到状态为inprocess_cancel"),
    
    /**
     * 未知状态
     */
    UNKNOWN("unknown", "未知状态");

    /**
     * 状态代码
     */
    private final String code;
    
    /**
     * 状态描述
     */
    private final String desc;

    /**
     * 构造函数
     * 
     * @param code 状态代码
     * @param desc 状态描述
     */
    QTechBookingStatusEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 获取状态代码
     * 
     * @return 状态代码
     */
    public String getCode() {
        return code;
    }

    /**
     * 获取状态描述
     * 
     * @return 状态描述
     */
    public String getDesc() {
        return desc;
    }

    /**
     * 根据状态代码获取枚举实例
     * 
     * @param code 状态代码
     * @return 对应的枚举实例，如果未找到则返回UNKNOWN
     */
    public static QTechBookingStatusEnum fromCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return UNKNOWN;
        }
        
        String normalizedCode = code.toLowerCase().trim();
        
        for (QTechBookingStatusEnum status : values()) {
            if (status.getCode().equals(normalizedCode)) {
                return status;
            }
        }
        
        return UNKNOWN;
    }

    /**
     * 判断状态是否需要继续轮询
     * 
     * @return 是否需要继续轮询
     */
    public boolean shouldContinuePolling() {
        switch (this) {
            case ON_REQUEST:
            case INPROCESS_BOOKING:
                return true;
            case VOUCHERED:
            case REJECTED:
            case FAILED:
            case INPROCESS_CANCEL:
                return false;
            default:
                return true; // 未知状态继续轮询
        }
    }

    @Override
    public String toString() {
        return String.format("QTechBookingStatusEnum{code='%s', desc='%s'}", code, desc);
    }
}
