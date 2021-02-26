/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util.invoke;

import com.oracle.coherence.common.internal.util.CanonicalNames;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.coherence.config.Config;

import com.tangosol.internal.util.invoke.lambda.AbstractRemotableLambda;
import com.tangosol.internal.util.invoke.lambda.LambdaIdentity;
import com.tangosol.internal.util.invoke.lambda.MethodReferenceExtractor;
import com.tangosol.internal.util.invoke.lambda.RemotableLambdaGenerator;
import com.tangosol.internal.util.invoke.lambda.MethodReferenceIdentity;
import com.tangosol.internal.util.invoke.lambda.AnonymousLambdaIdentity;
import com.tangosol.internal.util.invoke.lambda.StaticLambdaInfo;

import java.io.Serializable;

import java.lang.invoke.MethodHandleInfo;
import java.lang.invoke.SerializedLambda;

import java.lang.reflect.Method;

import java.util.Set;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Helpers for lambda remoting.
 *
 * @author as  2015.02.18
 * @since 12.2.1
 */
public abstract class Lambdas
    {
    /**
     * Return true if the specified object is a raw lambda.
     *
     * @param o  object to inspect
     *
     * @return true if object is a raw lambda
     */
    public static boolean isLambda(Object o)
        {
        return o != null && isLambdaClass(o.getClass());
        }

    /**
     * Return true if the specified Class represents a raw lambda.
     *
     * @param clz  class to inspect
     *
     * @return true if the class represents a raw lambda
     */
    public static boolean isLambdaClass(Class<?> clz)
        {
        return clz != null && clz.getName().contains("$$Lambda$");
        }

    /**
     * Return a {@link SerializedLambda} based upon the provided lambda, or
     * throw an IllegalArgumentException if the provided lambda is deemed to
     * not be a lambda.
     *
     * @param oLambda  the lambda that the returned SerializedLambda represents
     *
     * @return SerializedLambda representing the provided lambda
     *
     * @throws IllegalArgumentException  if the provided lambda is deemed not to be a lambda
     * @throws IllegalStateException     if an issue arose in creating the SerializedLambda
     */
    public static SerializedLambda getSerializedLambda(Object oLambda)
        {
        if (!isLambda(oLambda) || !(oLambda instanceof Serializable))
            {
            throw new IllegalArgumentException(
                "Specified object is not an instance of a serializable lambda");
            }

        try
            {
            Class<?> clzLambda = oLambda.getClass();
            Method   method    = clzLambda.getDeclaredMethod("writeReplace");
            method.setAccessible(true);

            return (SerializedLambda) method.invoke(oLambda);
            }
        catch (Exception e)
            {
            throw new IllegalStateException(
                "Unable to extract SerializedLambda from lambda: " + oLambda, e);
            }
        }

    /**
     * Return whether the provided method name is a name used by the JDK to
     * generate lambda implementations in the capturing class.
     *
     * @param sMethodName  the method name to inspect
     *
     * @return whether the provided method name represents a lambda implementation
     */
    public static boolean isLambdaMethod(String sMethodName)
        {
        return sMethodName.startsWith("lambda$");
        }

    /**
     * Return true if the provided {@link SerializedLambda} represents a method
     * reference.
     *
     * @param lambda  the SerializedLambda to inspect
     *
     * @return whether the SerializedLambda represents a method reference
     */
    public static boolean isMethodReference(SerializedLambda lambda)
        {
        return lambda.getImplMethodKind() == MethodHandleInfo.REF_newInvokeSpecial ||
               (!isLambdaMethod(lambda.getImplMethodName()) && lambda.getCapturedArgCount() == 0);
        }

    /**
     * Create a {@link ClassDefinition} for the provided raw Serializable lambda.
     *
     * @param id      the remote class identity
     * @param lambda  the raw Serializable lambda
     * @param loader  the ClassLoader used to load the capturing class containing
     *                the lambda implementation
     *
     * @return a LambdaDefinition that represents the Serializable lambda
     */
    public static ClassDefinition createDefinition(ClassIdentity id, Serializable lambda, ClassLoader loader)
        {
        SerializedLambda lambdaMetadata = getSerializedLambda(lambda);

        if (lambdaMetadata.getImplMethodKind() == MethodHandleInfo.REF_invokeStatic ||
            isMethodReference(lambdaMetadata))
            {
            ClassDefinition definition = new ClassDefinition(
                    id,
                    RemotableLambdaGenerator.createRemoteLambdaClass(id.getName(), lambdaMetadata, loader));

            definition.dumpClass(DUMP_LAMBDAS);

            return definition;
            }
        throw new IllegalArgumentException(
                "The specified lambda is referring to the enclosing class instance " +
                "or its fields and therefore cannot be marshalled across network boundaries (" +
                lambdaMetadata + ")");
        }

    /**
     * Return a {@link LambdaIdentity} based upon the provided
     * {@link SerializedLambda function metadata}.
     *
     * @param lambdaMetadata  function metadata
     * @param loader          ClassLoader used to load the capturing class
     *
     * @return an AbstractLambdaIdentity based upon the provided function metadata
     */
    public static LambdaIdentity createIdentity(SerializedLambda lambdaMetadata, ClassLoader loader)
        {
        return isMethodReference(lambdaMetadata)
               ? new MethodReferenceIdentity(lambdaMetadata, loader)
               : new AnonymousLambdaIdentity(lambdaMetadata, loader);
        }

    /**
     * Return captured arguments as an object array.
     *
     * @param lambdaMetadata  function metadata
     *
     * @return captured arguments
     */
    public static Object[] getCapturedArguments(SerializedLambda lambdaMetadata)
        {
        int      c      = lambdaMetadata.getCapturedArgCount();
        Object[] aoArgs = new Object[c];

        for (int i = 0; i < c; i++)
            {
            aoArgs[i] = lambdaMetadata.getCapturedArg(i);
            }

        return aoArgs;
        }

    /**
     * Ensure that the specified function is an instance of a {@link Remotable}
     * lambda.
     * <p>
     * If the specified function is not a lambda or if {@link #isDynamicLambdas() dynamic lambdas}
     * are not enabled, it will be ignored and returned as is, which makes this method safe to use
     * in the cases where the argument could be either a lambda or an instance of a concrete class.
     * Of course, this assumes that the specified {@code function} does not need to implement
     * {@code Remotable} interface because it provides native support for
     * serialization and can be marshaled across process boundaries on its own.
     *
     * @param <T>       the type of the function
     * @param function  the function to convert into {@code Remotable} lambda
     *                  if necessary
     *
     * @return a {@code Remotable} lambda for the specified function, or the
     *         function itself if the specified function is not a lambda or dynamic
     *         lambdas are not enabled
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T extends Serializable> T ensureRemotable(T function)
        {
        if (function instanceof MethodReferenceExtractor ||
            function instanceof AbstractRemotableLambda ||
            !isLambda(function))
            {
            return function;
            }

        SerializedLambda serializedLambda = getSerializedLambda(function);
        if (isMethodReference(serializedLambda) &&
            serializedLambda.getImplMethodKind() == MethodHandleInfo.REF_invokeVirtual &&
            EXTRACTOR_INTERFACES.contains(serializedLambda.getFunctionalInterfaceClass()))
            {
            return (T) new MethodReferenceExtractor(serializedLambda);
            }

        if (isStaticLambdas())
            {
            return function;
            }

        RemotableSupport support = RemotableSupport.get(function.getClass().getClassLoader());
        return support.realize(support.createRemoteConstructor(function));
        }

    /**
     * Return lambda serialization wire format based on {@link #LAMBDAS_SERIALIZATION_MODE} value.
     *
     * @param oFunction  raw lambda to convert into serializable wire format
     *
     * @return a dynamic or static lambda serialization wire format for lambda oFunction or just
     *         oFunction itself if the specified oFunction is not a lambda
     *
     * @see #LAMBDAS_SERIALIZATION_MODE
     *
     * @since 14.1.1.0.2
     */
    public static Object ensureSerializable(Object oFunction)
        {
        return isLambda(oFunction)
                ? isDynamicLambdas()
                    ? ensureRemotable((Serializable) oFunction)
                    : new StaticLambdaInfo(oFunction.getClass(), oFunction)
                : oFunction;
        }

    /**
     * Return the canonical name for a given lambda object.  If the object is not a lambda,
     * return null.  This is for the Coherence ValueExtractor.
     *
     * @param oLambda  the lambda object
     *
     * @return the lambda's canonical name, or null if the supplied object is not a lambda
     *
     * @since 12.2.1.4
     */
    public static String getValueExtractorCanonicalName(Object oLambda)
        {
        if (oLambda instanceof AbstractRemotableLambda)
            {
            return ((AbstractRemotableLambda<?>) oLambda).getCanonicalName();
            }
        else
            {
            SerializedLambda lambda = Lambdas.getSerializedLambda(oLambda);
            if (Lambdas.isMethodReference(lambda))
                {
                MethodReferenceIdentity id = new MethodReferenceIdentity(lambda, null);
                return CanonicalNames.computeValueExtractorCanonicalName(id.getImplMethod() + CanonicalNames.VALUE_EXTRACTOR_METHOD_SUFFIX, null);
                }
            }

        return null;
        }

    /**
     * Return {@code true} only if lambdas are serialized using dynamic lambdas.
     *
     * @return {@code true} only if lambdas are serialized using dynamic lambdas
     *
     * @since 14.1.1.0.2
     */
    public static boolean isDynamicLambdas()
        {
        return LAMBDAS_SERIALIZATION_MODE == SerializationMode.DYNAMIC;
        }

    /**
     * Return {@code true} only if lambdas are serialized using static lambdas.
     *
     * @return {@code true} only if lambdas are serialized using static lambdas
     *
     * @since 14.1.1.0.2
     */
    public static boolean isStaticLambdas()
        {
        return LAMBDAS_SERIALIZATION_MODE == SerializationMode.STATIC;
        }

    // ----- inner enum: SerializationMode ----------------------------------

    /**
     * Specify the serialization mode for lambdas.
     *
     * @since 14.1.1.0.2
     */
    public static enum SerializationMode
        {
        /**
         * Serialize lambdas as dynamic lambdas.
         */
        DYNAMIC,

        /**
         * Serialize lambdas as static lambdas.
         */
        STATIC
       };

    // ---- static members --------------------------------------------------

    /**
     * Path to the file system where generated lambdas should be written; purely for debug.
     */
    private static final String DUMP_LAMBDAS = Config.getProperty("coherence.remotable.dumpLambdas",
                                                                  Config.getProperty("coherence.remotable.dumpAll"));

    /**
     * System property to control lambda serialization. Valid values are defined by enumeration {@link SerializationMode}.
     *
     * @since 14.1.1.0.2
     */
    public static final String LAMBDAS_SERIALIZATION_MODE_PROPERTY = "coherence.lambdas";

    /**
     * Specifies {@link SerializationMode} used for lambdas.
     * System property {@link #LAMBDAS_SERIALIZATION_MODE_PROPERTY} configures this setting.
     * The default configuration {@link SerializationMode#DYNAMIC} enables dynamic lambdas serialization.
     *
     * @since 14.1.1.0.2
     */
    protected static final SerializationMode LAMBDAS_SERIALIZATION_MODE;

    private static final Set<String> EXTRACTOR_INTERFACES;

    static
        {
        String            sPropValue = null;
        SerializationMode mode       = SerializationMode.DYNAMIC;
        try
            {
            sPropValue = Config.getProperty(LAMBDAS_SERIALIZATION_MODE_PROPERTY, SerializationMode.DYNAMIC.name());
            mode       = SerializationMode.valueOf(sPropValue.toUpperCase());
            }
        catch (IllegalArgumentException e)
            {
            StringBuilder sbValues = new StringBuilder("[");

            for (SerializationMode m : SerializationMode.values())
                {
                sbValues.append(m).append("|");
                }
            sbValues.setCharAt(sbValues.length() - 1, ']');

            final String            f_sPropValue = sPropValue;
            final SerializationMode f_mode       = mode;

            Logger.warn(() ->
                "System property \"" + LAMBDAS_SERIALIZATION_MODE_PROPERTY + "\" is set to invalid value of \"" + f_sPropValue + "\"" +
                    "; valid values are: " + sbValues.toString() +
                    ". Reverting to default mode of " + f_mode.name() + ".");
            }
        LAMBDAS_SERIALIZATION_MODE = mode;

        EXTRACTOR_INTERFACES = Stream.of(
                    "com/tangosol/util/ValueExtractor",
                    "com/tangosol/util/function/Remote$Function",
                    "com/tangosol/util/function/Remote$ToIntFunction",
                    "com/tangosol/util/function/Remote$ToLongFunction",
                    "com/tangosol/util/function/Remote$ToDoubleFunction"
            ).collect(Collectors.toSet());
        }
    }
