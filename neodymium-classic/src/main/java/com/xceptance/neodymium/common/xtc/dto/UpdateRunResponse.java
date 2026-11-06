package com.xceptance.neodymium.common.xtc.dto;

// Data Transfer Object for updating an existing test run in the XTC API.
public class UpdateRunResponse
{
    private final Boolean success;

    private final ResponseData data;

    public UpdateRunResponse(Boolean success, ResponseData data)
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
