package com.logic.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class InvoiceDTO {
    private String fileName;
    private byte[] content;
}
