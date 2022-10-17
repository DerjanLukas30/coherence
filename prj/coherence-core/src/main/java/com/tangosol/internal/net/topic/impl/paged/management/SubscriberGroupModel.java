/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged.management;

import com.tangosol.internal.net.management.model.AbstractModel;
import com.tangosol.internal.net.management.model.ModelAttribute;
import com.tangosol.internal.net.management.model.SimpleModelAttribute;
import com.tangosol.internal.net.topic.impl.paged.PagedTopic;
import com.tangosol.internal.net.topic.impl.paged.statistics.PagedTopicStatistics;
import com.tangosol.internal.net.topic.impl.paged.statistics.SubscriberGroupStatistics;

import javax.management.DynamicMBean;

import com.tangosol.util.Filter;

import java.util.Objects;
import java.util.function.Function;

/**
 * An MBean model for a {@link PagedTopic}
 *
 * @author Jonathan Knight 2022.09.10
 * @since 23.03
 */
public class SubscriberGroupModel
        extends AbstractModel<SubscriberGroupModel>
        implements DynamicMBean, PolledMetrics
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a {@link SubscriberGroupModel}.
     *
     * @param pagedTopicStatistics the topic this model represents
     * @param sGroupName           the name of the subscriber group
     * @param filter               the filter used to filter messages to be received by subscribers in the group
     * @param fnConvert            the Function used to convert messages to be received by subscribers in the group
     */
    public SubscriberGroupModel(PagedTopicStatistics pagedTopicStatistics, String sGroupName, Filter<?> filter, Function<?, ?> fnConvert)
        {
        super(MBEAN_DESCRIPTION);
        f_sGroupName = sGroupName;
        f_filter     = filter;
        f_fnConvert  = fnConvert;
        f_cChannel   = pagedTopicStatistics.getChannelCount();
        f_statistics = pagedTopicStatistics;

        // create the array of channel models
        f_aChannel = new SubscriberGroupChannelModel[f_cChannel];
        for (int nChannel = 0; nChannel < f_cChannel; nChannel++)
            {
            f_aChannel[nChannel] = new SubscriberGroupChannelModel(f_statistics, sGroupName, nChannel);
            }

        // configure the attributes of the MBean
        addAttribute(ATTRIBUTE_CHANNEL_COUNT);
        addAttribute(ATTRIBUTE_POLLED_COUNT);
        addAttribute(ATTRIBUTE_POLLED_MEAN);
        addAttribute(ATTRIBUTE_POLLED_ONE_MINUTE);
        addAttribute(ATTRIBUTE_POLLED_FIVE_MINUTE);
        addAttribute(ATTRIBUTE_POLLED_FIFTEEN_MINUTE);
        addAttribute(ATTRIBUTE_CHANNEL_TABLE);
        addAttribute(ATTRIBUTE_FILTER);
        addAttribute(ATTRIBUTE_TRANSFORMER);
        }

    // ----- PagedTopicModel methods ----------------------------------------

    /**
     * Returns the channel count.
     *
     * @return the channel count
     */
    protected int getChannelCount()
        {
        return f_cChannel;
        }

    /**
     * Returns the filter for the subscriber group.
     *
     * @return  the filter
     */
    protected String getFilter()
        {
        return Objects.toString(f_filter, null);
        }

    /**
     * Returns the transformer for the subscriber group.
     *
     * @return  the transformer
     */
    protected String getTransformer()
        {
        return Objects.toString(f_fnConvert, null);
        }

    /**
     * Return the {@link SubscriberGroupChannelModel} for a specific channel.
     * <p>
     * The channel parameter is a zero based index of channels and must be
     * greater than or equal to 0 and less than the channel count.
     *
     * @param nChannel  the channel to obtain the model for
     *
     * @return the {@link SubscriberGroupChannelModel} for the channel
     *
     * @throws IndexOutOfBoundsException if the channel parameter is less than zero
     *         or greater than or equal to the channel count
     */
    protected SubscriberGroupChannelModel getChannelModel(int nChannel)
        {
        return f_aChannel[nChannel];
        }

    // ----- PolledMetrics methods ---------------------------------------

    @Override
    public long getPolledCount()
        {
        return getStatistics().getPolledCount();
        }

    @Override
    public double getPolledFifteenMinuteRate()
        {
        return getStatistics().getPolledFifteenMinuteRate();
        }

    @Override
    public double getPolledFiveMinuteRate()
        {
        return getStatistics().getPolledFiveMinuteRate();
        }

    @Override
    public double getPolledOneMinuteRate()
        {
        return getStatistics().getPolledOneMinuteRate();
        }

    @Override
    public double getPolledMeanRate()
        {
        return getStatistics().getPolledMeanRate();
        }

    // ----- helper methods -------------------------------------------------

    protected SubscriberGroupStatistics getStatistics()
        {
        return f_statistics.getSubscriberGroupStatistics(f_sGroupName);
        }

    // ----- constants ------------------------------------------------------

    /**
     * The MBean's description.
     */
    protected static final String MBEAN_DESCRIPTION = "A Coherence PagedTopic";

    /**
     * The channel count attribute.
     */
    protected static final ModelAttribute<SubscriberGroupModel> ATTRIBUTE_CHANNEL_COUNT =
            SimpleModelAttribute.intBuilder("ChannelCount", SubscriberGroupModel.class)
                    .withDescription("The number of channels in the topic")
                    .withFunction(SubscriberGroupModel::getChannelCount)
                    .metric(false)
                    .build();

    /**
     * The polled count attribute.
     */
    protected static final ModelAttribute<SubscriberGroupModel> ATTRIBUTE_POLLED_COUNT =
            PolledMetrics.ATTRIBUTE_COUNT.asBuilder(SubscriberGroupModel.class)
                    .withFunction(SubscriberGroupModel::getPolledCount)
                    .build();

    /**
     * The polled mean rate attribute.
     */
    protected static final ModelAttribute<SubscriberGroupModel> ATTRIBUTE_POLLED_MEAN =
            PolledMetrics.ATTRIBUTE_MEAN_RATE.asBuilder(SubscriberGroupModel.class)
                    .withFunction(SubscriberGroupModel::getPolledMeanRate)
                    .build();

    /**
     * The polled one-minute rate attribute.
     */
    protected static final ModelAttribute<SubscriberGroupModel> ATTRIBUTE_POLLED_ONE_MINUTE =
            PolledMetrics.ATTRIBUTE_ONE_MINUTE_RATE.asBuilder(SubscriberGroupModel.class)
                    .withFunction(SubscriberGroupModel::getPolledOneMinuteRate)
                    .build();

    /**
     * The polled five-minute rate attribute.
     */
    protected static final ModelAttribute<SubscriberGroupModel> ATTRIBUTE_POLLED_FIVE_MINUTE =
            PolledMetrics.ATTRIBUTE_FIVE_MINUTE_RATE.asBuilder(SubscriberGroupModel.class)
                    .withFunction(SubscriberGroupModel::getPolledFiveMinuteRate)
                    .build();

    /**
     * The polled fifteen-minute rate attribute.
     */
    protected static final ModelAttribute<SubscriberGroupModel> ATTRIBUTE_POLLED_FIFTEEN_MINUTE =
            PolledMetrics.ATTRIBUTE_FIFTEEN_MINUTE_RATE.asBuilder(SubscriberGroupModel.class)
                    .withFunction(SubscriberGroupModel::getPolledFifteenMinuteRate)
                    .build();

    /**
     * The channel attributes table.
     */
    protected static final ModelAttribute<SubscriberGroupModel> ATTRIBUTE_CHANNEL_TABLE
            = new SubscriberGroupChannelTableModel();

    /**
     * The filter attribute.
     */
    protected static final ModelAttribute<SubscriberGroupModel> ATTRIBUTE_FILTER =
            SimpleModelAttribute.stringBuilder("Filter", SubscriberGroupModel.class)
                    .withDescription("The filter")
                    .withFunction(SubscriberGroupModel::getFilter)
                    .build();

    /**
     * The transformer attribute.
     */
    protected static final ModelAttribute<SubscriberGroupModel> ATTRIBUTE_TRANSFORMER =
            SimpleModelAttribute.stringBuilder("Transformer", SubscriberGroupModel.class)
                    .withDescription("The transformer")
                    .withFunction(SubscriberGroupModel::getTransformer)
                    .build();

    // ----- data members ---------------------------------------------------

    /**
     * The topic statistics.
     */
    private final PagedTopicStatistics f_statistics;

    /**
     * The name of the subscriber group.
     */
    private final String f_sGroupName;

    /**
     * The filter used to filter messages to be received by subscribers in the group.
     */
    private final Filter<?> f_filter;

    /**
     * the Function used to convert messages to be received by subscribers in the group.
     */
    private final Function<?, ?> f_fnConvert;

    /**
     * The channel count;
     */
    private final int f_cChannel;

    /**
     * The channel models.
     */
    private final SubscriberGroupChannelModel[] f_aChannel;
    }
