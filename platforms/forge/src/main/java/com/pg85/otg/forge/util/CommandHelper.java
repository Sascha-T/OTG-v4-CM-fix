package com.pg85.otg.forge.util;

public class CommandHelper
{
    public static boolean containsArgument(String[] args, String arg)
    {
        for (String str : args)
        {
            if (str.equalsIgnoreCase(arg))
            {
                return true;
            }
        }

        return false;
    }
}
