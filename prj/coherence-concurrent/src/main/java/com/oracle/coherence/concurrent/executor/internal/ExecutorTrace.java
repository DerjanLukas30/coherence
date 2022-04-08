/*
 * Copyright (c) 2016, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.concurrent.executor.internal;

import com.oracle.coherence.common.base.Logger;

import com.oracle.coherence.concurrent.executor.ClusteredRegistration;

import com.oracle.coherence.concurrent.executor.options.Debugging;

import java.util.function.Supplier;

/**
 * Utility class used to trace executor processors, subscribers, and tasks.
 *
 * @author lh
 * @since 21.12
 */
public class ExecutorTrace
    {
    // ----- public static methods ------------------------------------------

    /**
     * Return true if executor trace logging is enabled; false otherwise.
     *
     * @return true if executor trace logging is enabled
     */
    public static boolean isEnabled()
        {
        return ClusteredRegistration.s_fTraceLogging;
        }

    /**
     * Log the specified message at the Debugging severity level. Default
     * severity level is Logger.FINEST.
     *
     * @param message  the message to log
     */
    public static void log(String message)
        {
        if (isEnabled())
            {
            Logger.log(message, LOGLEVEL);
            }
        }

    /**
     * Log the specified message with the given debugging option.
     *
     * @param message    the message to log
     * @param debugging  the debugging option
     */
    public static void log(String message, Debugging debugging)
        {
        Logger.log(message, debugging.getLogLevel());
        }

    /**
     * Log the specified message at the Debugging severity level. Default
     * severity level is Logger.FINEST.
     *
     * The message is provided by the {@link Supplier}, which will only be
     * evaluated if the messages should be logged at the specified severity
     * level. This avoids potentially expensive message construction if the
     * message isn't going to be logged.
     *
     * @param supplierMessage  the supplier of the message to log; only evaluated
     *                         if the Debugging severity level should be logged
     */
    public static void log(Supplier<String> supplierMessage)
        {
        if (isEnabled())
            {
            Logger.log(supplierMessage, LOGLEVEL);
            }
        }

    /**
     * Log the specified message with the given debugging option.
     *
     * The message is provided by the {@link Supplier}, which will only be
     * evaluated if the messages should be logged at the specified severity
     * level. This avoids potentially expensive message construction if the
     * message isn't going to be logged.
     *
     * @param supplierMessage  the supplier of the message to log; only evaluated
     *                         if the specified severity level should be logged
     * @param debugging        the debugging option
     */
    public static void log(Supplier<String> supplierMessage, Debugging debugging)
        {
        Logger.log(supplierMessage, debugging.getLogLevel());
        }

    /**
     * Entry logging.
     *
     * @param clz      the source {@link Class}
     * @param sMethod  the source method
     * @param params   zero or more parameters to log
     *
     * @throws NullPointerException if either {@code clz} or {@code sMethod}
     *                              are {@code null}
     */
    public static void entering(Class<?> clz, String sMethod, Object... params)
        {
        if (isEnabled())
            {
            Logger.entering(clz, sMethod, params);
            }
        }

    /**
     * Exit logging.
     *
     * @param clz      the source {@link Class}
     * @param sMethod  the source method
     *
     * @throws NullPointerException if either {@code clz} or {@code sMethod}
     *                              are {@code null}
     */
    public static void exiting(Class<?> clz, String sMethod)
        {
        if (isEnabled())
            {
            Logger.exiting(clz, sMethod);
            }
        }

    /**
     * Exit logging.
     *
     * @param clz            the source {@link Class}
     * @param sMethod        the source method
     * @param result         the result returned by the exiting method
     * @param additionalInfo zero or more additional state details at the time of exit
     *
     * @throws NullPointerException if either {@code clz} or {@code sMethod}
     *                              are {@code null}
     */
    public static void exiting(Class<?> clz, String sMethod, Object result, Object... additionalInfo)
        {
        if (isEnabled())
            {
            Logger.exiting(clz, sMethod, result, additionalInfo);
            }
        }

    /**
     * Throwable logging.
     *
     * @param clz             the source {@link Class}
     * @param sMethod         the source method
     * @param throwable       the {@link Exception} being thrown
     * @param additionalInfo  zero or more additional state details at the time of exit
     *
     * @throws NullPointerException if any arguments are {@code null}
     */
    public static void throwing(Class<?> clz, String sMethod, Throwable throwable, Object... additionalInfo)
        {
       if (isEnabled())
           {
           Logger.throwing(clz, sMethod, throwable, additionalInfo);
           }
        }

    // ----- static data members --------------------------------------------

    /**
     * Log level for ExecutorTrace messages
     */
    public static final int LOGLEVEL = Logger.FINEST;
    }
