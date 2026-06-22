package com.ssafy.zipdaum.property.batch.wrapper;

import com.ssafy.zipdaum.property.dto.PropertySaveCommand;
import com.ssafy.zipdaum.property.dto.RentDealSaveCommand;
import com.ssafy.zipdaum.property.dto.SaleDealSaveCommand;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PropertyDealWrapper {

    private PropertySaveCommand property;

    private SaleDealSaveCommand saleDeal;
    private RentDealSaveCommand rentDeal;

    public boolean isSale() {
        return saleDeal != null;
    }

}
