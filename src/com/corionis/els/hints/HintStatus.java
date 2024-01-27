package com.corionis.els.hints;

import java.io.Serializable;

public class HintStatus implements Serializable
{
    public String system = null;
    public String status = null; // For, Done

    public HintStatus()
    {
    }

    public HintStatus(String system, String status)
    {
        this.system = system;
        this.status = status;
    }

}
