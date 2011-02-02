/*
 * This is part of Geomajas, a GIS framework, http://www.geomajas.org/.
 *
 * Copyright 2008-2011 Geosparc nv, http://www.geosparc.com/, Belgium.
 *
 * The program is available in open source according to the GNU Affero
 * General Public License. All contributions in this program are covered
 * by the Geomajas Contributors License Agreement. For full licensing
 * details, see LICENSE.txt in the project root.
 */

package org.geomajas.service.pipeline;

import org.geomajas.global.Api;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

/**
 * Configuration info for a pipeline service.
 *
 * @param <RESPONSE> type of response object for the pipeline
 *
 * @author Joachim Van der Auwera
 * @since 1.6.0
 */
@Api(allMethods = true)
public class PipelineInfo<RESPONSE> {

	@NotNull
	private String pipelineName;

	private String layerId;

	private PipelineInfo delegatePipeline;

	private List<PipelineStep<RESPONSE>> pipeline;

	private Map<String, PipelineStep<RESPONSE>> extensions;

	private List<PipelineInterceptor<RESPONSE>> interceptors;

	
	
	
	
	
	
	
	
	
	
	/**
	 * Get the name of the pipeline for which this is an implementation.
	 *
	 * @return pipeline name
	 */
	public String getPipelineName() {
		return pipelineName;
	}

	/**
	 * Set the name of the pipeline for which steps are defined.
	 *
	 * @param pipelineName pipeline name
	 */
	public void setPipelineName(String pipelineName) {
		this.pipelineName = pipelineName;
	}

	/**
	 * Set the layer for which this definition should apply.
	 * <p/>
	 * When this is not set (or null), it indicates a candidate default implementation.
	 *
	 * @return layer for which this pipeline applies
	 */
	public String getLayerId() {
		return layerId;
	}

	/**
	 * Get the layer id for which this pipeline applies.
	 * <p/>
	 * When this is not set (or null), it indicates a candidate default implementation.
	 *
	 * @param layerId layer for which this pipeline applies
	 */
	public void setLayerId(String layerId) {
		this.layerId = layerId;
	}

	/**
	 * Get the delegate pipeline.
	 * <p/>
	 * When this is set, the pipeline for the delegate should be used instead of the pipeline defined in this bean.
	 *
	 * @return delegate pipeline definition
	 */
	public PipelineInfo getDelegatePipeline() {
		return delegatePipeline;
	}

	/**
	 * Define a delegate pipeline.
	 * <p/>
	 * When this is set, the pipeline for the delegate should be used instead of the pipeline defined in this bean.
	 *
	 * @param delegatePipeline delegate pipeline definition
	 */
	public void setDelegatePipeline(PipelineInfo delegatePipeline) {
		this.delegatePipeline = delegatePipeline;
	}

	/**
	 * Get the list of steps which form the pipeline.
	 *
	 * @return list of pipeline steps
	 */
	public List<PipelineStep<RESPONSE>> getPipeline() {
		return pipeline;
	}

	/**
	 * Set the list of steps which form the pipeline.
	 *
	 * @param pipeline list of pipeline steps
	 */
	public void setPipeline(List<PipelineStep<RESPONSE>> pipeline) {
		this.pipeline = pipeline;
	}

	/**
	 * Get set of steps which need to be weaved at the extension hooks.
	 *
	 * @return map of hook name and pipeline step
	 * @since 1.7.0
	 */
	public Map<String, PipelineStep<RESPONSE>> getExtensions() {
		return extensions;
	}

	/**
	 * Set the set of steps which need to be weaved at the extension hooks.
	 *
	 * @param extensions map of hook name and pipeline step
	 * @since 1.7.0
	 */
	public void setExtensions(Map<String, PipelineStep<RESPONSE>> extensions) {
		this.extensions = extensions;
	}
	
	/**
	 * Get the list of interceptors to be added to thie pipeline.
	 * 
	 * @return list of interceptors
	 * @since 1.9.0
	 */
	public List<PipelineInterceptor<RESPONSE>> getInterceptors() {
		return interceptors;
	}

	/**
	 * Set the set of steps which need to be weaved at the extension hooks.
	 * 
	 * @param interceptors
	 *            list of interceptors
	 * @since 1.9.0
	 */
	public void setInterceptors(List<PipelineInterceptor<RESPONSE>> interceptors) {
		this.interceptors = interceptors;
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	

}
