package com.fedex.assessment.controller;

import com.fedex.assessment.model.Result;
import com.fedex.assessment.model.validation.IsoCountryCode;
import com.fedex.assessment.service.AggregationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.ConstraintViolationException;
import javax.validation.constraints.Size;
import java.util.List;

@RestController
@Validated
@Api(value = "Aggregation REST Controller", description = "Get aggregated response from pricing, track and shipment services")
public class AggregationController {
    private static Logger logger = LoggerFactory.getLogger(AggregationController.class);


    @Autowired
    private AggregationService aggregationService;
    @RequestMapping(path= "/aggregation", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get aggregated response from pricing, track and shipment services", response = Result.class)
    public Result getAggregation(
            @ApiParam(value = "ISO country code list")  @RequestParam(name = "pricing", required = false) List<@IsoCountryCode  String> pricing,
            @ApiParam(value = "9-digit order numbers list")  @RequestParam(name="track", required = false) List<@Size(min = 9, max = 9)  String> tracking,
            @ApiParam(value = "9-digit order numbers list")  @RequestParam(name = "shipments", required = false) List<@Size(min = 9, max = 9) String> shipments){
        logger.info("received request. pricing: {} track: {} shipments: {}", pricing, tracking, shipments);
        return aggregationService.getAggregation(pricing, tracking, shipments);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<String> onValidationError(Exception ex) {
        return new ResponseEntity<>(ex.getLocalizedMessage(), HttpStatus.BAD_REQUEST);
    }
}
