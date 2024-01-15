
package org.open4goods.api.services;

import java.util.ArrayList;
import java.util.List;

import org.open4goods.api.config.yml.ApiProperties;
import org.open4goods.api.services.aggregation.AbstractAggregationService;
import org.open4goods.api.services.aggregation.aggregator.RealTimeAggregator;
import org.open4goods.api.services.aggregation.services.realtime.AttributeRealtimeAggregationService;
import org.open4goods.api.services.aggregation.services.realtime.BarCodeAggregationService;
import org.open4goods.api.services.aggregation.services.realtime.DescriptionsAggregationService;
import org.open4goods.api.services.aggregation.services.realtime.IdAggregationService;
import org.open4goods.api.services.aggregation.services.realtime.MediaAggregationService;
import org.open4goods.api.services.aggregation.services.realtime.NamesAggregationService;
import org.open4goods.api.services.aggregation.services.realtime.PriceAggregationService;
import org.open4goods.api.services.aggregation.services.realtime.TaxonomyRealTimeAggregationService;
import org.open4goods.config.yml.ui.VerticalConfig;
import org.open4goods.dao.ProductRepository;
import org.open4goods.exceptions.AggregationSkipException;
import org.open4goods.model.data.DataFragment;
import org.open4goods.model.product.Product;
import org.open4goods.services.BarcodeValidationService;
import org.open4goods.services.BrandService;
import org.open4goods.services.DataSourceConfigService;
import org.open4goods.services.EvaluationService;
import org.open4goods.services.GoogleTaxonomyService;
import org.open4goods.services.Gs1PrefixService;
import org.open4goods.services.StandardiserService;
import org.open4goods.services.VerticalsConfigService;
import org.open4goods.services.textgen.BlablaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;

import jakarta.annotation.PreDestroy;

/**
 * This service is in charge of building Product in realtime mode
 *
 * @author goulven
 *
 */
public class RealtimeAggregationService {

	protected static final Logger logger = LoggerFactory.getLogger(RealtimeAggregationService.class);

	private EvaluationService evaluationService;

	private StandardiserService standardiserService;

	private AutowireCapableBeanFactory autowireBeanFactory;

	private ProductRepository aggregatedDataRepository;

	private ApiProperties apiProperties;

	private Gs1PrefixService gs1prefixService;

	private DataSourceConfigService dataSourceConfigService;

	private VerticalsConfigService verticalConfigService;

	private RealTimeAggregator aggregator;

	private BarcodeValidationService barcodeValidationService;

	private BrandService brandService;
	
	private GoogleTaxonomyService taxonomyService;

	private BlablaService blablaService;
	
	public RealtimeAggregationService(EvaluationService evaluationService,
			StandardiserService standardiserService,
			AutowireCapableBeanFactory autowireBeanFactory, ProductRepository aggregatedDataRepository,
			ApiProperties apiProperties, Gs1PrefixService gs1prefixService,
			DataSourceConfigService dataSourceConfigService, VerticalsConfigService configService,
			BarcodeValidationService barcodeValidationService,
			BrandService brandService,
			GoogleTaxonomyService taxonomyService,
			BlablaService blablaService
			) {
		super();
		this.evaluationService = evaluationService;
		this.standardiserService = standardiserService;
		this.autowireBeanFactory = autowireBeanFactory;
		this.aggregatedDataRepository = aggregatedDataRepository;
		this.apiProperties = apiProperties;
		this.gs1prefixService = gs1prefixService;
		this.dataSourceConfigService = dataSourceConfigService;
		verticalConfigService = configService;
		this.brandService=brandService;
		this.barcodeValidationService = barcodeValidationService;
		this.taxonomyService = taxonomyService;
		this.blablaService = blablaService;
		aggregator = getAggregator(configService.getDefaultConfig());


		// Calling aggregator.BEFORE
		aggregator.beforeStart();

	}



	public Product process(DataFragment df, Product data) throws AggregationSkipException {
		return aggregator.build(df, data);
	}



	@PreDestroy
	public void shutdown() {
		aggregator.close();


	}

	/**
	 * List of services in the aggregator
	 *TODO : Replace confif with verticalConfigService, to have hot vertical config onDataFragment
	 * @param config
	 * @return
	 */
	RealTimeAggregator getAggregator(VerticalConfig config) {

		//		final CapsuleGenerationConfig config = generationConfig;

		if (null == config) {
			logger.error("No capsule generation config");
			return null;
		}

		final List<AbstractAggregationService> services = new ArrayList<>();

		services.add(new BarCodeAggregationService(apiProperties.logsFolder(), gs1prefixService,barcodeValidationService, apiProperties.isDedicatedLoggerToConsole()));

		services.add(new TaxonomyRealTimeAggregationService( apiProperties.logsFolder(), verticalConfigService, taxonomyService, apiProperties.isDedicatedLoggerToConsole()));

		services.add(new AttributeRealtimeAggregationService(verticalConfigService, brandService, apiProperties.logsFolder(), apiProperties.isDedicatedLoggerToConsole()));


		services.add(new NamesAggregationService(apiProperties.logsFolder(), apiProperties.isDedicatedLoggerToConsole(), verticalConfigService, evaluationService, blablaService));

		//		services.add(new CategoryService(apiProperties.logsFolder(), taxonomyService));



		services.add(new IdAggregationService( apiProperties.logsFolder(), apiProperties.isDedicatedLoggerToConsole()));

		//		services.add(new UrlsAggregationService(evaluationService, apiProperties.logsFolder(),
		//				config.getNamings().getProductUrlTemplates()));

		services.add(new PriceAggregationService(apiProperties.logsFolder(), dataSourceConfigService, apiProperties.isDedicatedLoggerToConsole()));

		//		services.add(new CommentsAggregationService(apiProperties.logsFolder(), config.getCommentsConfig()));
		//		services.add(new ProsAndConsAggregationService(apiProperties.logsFolder()));
		//		services.add(new QuestionsAggregationService(apiProperties.logsFolder()));

		services.add(new DescriptionsAggregationService(config.getDescriptionsAggregationConfig(),
				apiProperties.logsFolder(), apiProperties.isDedicatedLoggerToConsole()));


		services.add(new MediaAggregationService(config, apiProperties.logsFolder(), apiProperties.isDedicatedLoggerToConsole()));


	
		
		final RealTimeAggregator ret = new RealTimeAggregator(services);

		autowireBeanFactory.autowireBean(ret);

		return ret;
	}




}
