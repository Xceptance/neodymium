package com.xceptance.neodymium.common.xtc.dto;

// Data Transfer Object for the response of creating a new test run in the XTC API.
public class CreateRunResponse
{
    private final Boolean success;

    private final ResponseData data;

    public CreateRunResponse(Boolean success, ResponseData data)
    {
        this.success = success;
        this.data = data;
    }

    public Boolean getSuccess()
    {
        return success;
    }

    public ResponseData getData()
    {
        return data;
    }
}
