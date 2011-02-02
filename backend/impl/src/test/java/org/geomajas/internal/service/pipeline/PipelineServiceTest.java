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

package org.geomajas.internal.service.pipeline;

import junit.framework.Assert;
import org.geomajas.global.ExceptionCode;
import org.geomajas.global.GeomajasException;
import org.geomajas.layer.feature.attribute.StringAttribute;
import org.geomajas.service.pipeline.PipelineContext;
import org.geomajas.service.pipeline.PipelineInfo;
import org.geomajas.service.pipeline.PipelineService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Test for the correct functioning of {@link PipelineService}.
 *
 * @author Joachim Van der Auwera
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/org/geomajas/spring/geomajasContext.xml", 
		"/org/geomajas/internal/rendering/pipeline/pipelineContext.xml" })
public class PipelineServiceTest {

	@Autowired
	private PipelineService pipelineService;

	@Test
	public void testPipeline() throws Exception {
		StringAttribute response = new StringAttribute("bla");

		PipelineContext context = pipelineService.createContext();
		context.put("start", "start");
		pipelineService.execute("pipelineTest", null, context, response);
		Assert.assertEquals("starts1s2s3", response.getValue());

		context.put("start", "bla-");
		pipelineService.execute("pipelineTest", "aLayer", context, response);
		Assert.assertEquals("bla-step-1step-3", response.getValue());

		context.put("start", "stop-");
		pipelineService.execute("pipelineTest", "stop", context, response);
		Assert.assertEquals("stop-s1-STOP", response.getValue());
	}

	@Test
	public void testGetPipeline() throws Exception {
		PipelineInfo<StringAttribute> pipelineInfo;

		pipelineInfo = pipelineService.getPipeline("pipelineTest", "aLayer");
		Assert.assertNotNull(pipelineInfo);
		Assert.assertEquals("step-1", pipelineInfo.getPipeline().get(0).getId());

		pipelineInfo = pipelineService.getPipeline("pipelineTest", "bla");
		Assert.assertNotNull(pipelineInfo);
		Assert.assertEquals("s1", pipelineInfo.getPipeline().get(0).getId());

		pipelineInfo = pipelineService.getPipeline("pipelineTest", null);
		Assert.assertNotNull(pipelineInfo);
		Assert.assertEquals("s1", pipelineInfo.getPipeline().get(0).getId());

		try {
			pipelineInfo = pipelineService.getPipeline("bla", null);
			Assert.fail("exception should be thrown");
		} catch (GeomajasException ge) {
			Assert.assertEquals(ExceptionCode.PIPELINE_UNKNOWN, ge.getExceptionCode());
		}
	}

	@Test
	public void testDelegate() throws Exception {
		PipelineInfo<StringAttribute> pipelineInfo;

		pipelineInfo = pipelineService.getPipeline("pipelineTest", "delegate");
		Assert.assertNotNull(pipelineInfo);

		StringAttribute response = new StringAttribute("bla");
		PipelineContext context = pipelineService.createContext();
		context.put("start", "stop-");
		pipelineService.execute(pipelineInfo, context, response);
		Assert.assertEquals("stop-s1-STOP", response.getValue());
	}

	@Test
	public void extensionTest() throws Exception {
		StringAttribute response = new StringAttribute("bla");

		PipelineContext context = pipelineService.createContext();
		context.put("start", "start");
		pipelineService.execute("hookedTest", "base", context, response);
		Assert.assertEquals("starts1s2", response.getValue());

		context.put("start", "start");
		pipelineService.execute("hookedTest", "delegate2", context, response);
		Assert.assertEquals("starts1pps2ps2s2", response.getValue());

		context.put("start", "start");
		pipelineService.execute("hookedTest", "delegate", context, response);
		Assert.assertEquals("starts1ps2s2", response.getValue());
	}
	
	@Test
	public void interceptorTest() throws Exception {
		StringAttribute response = new StringAttribute("bla");
		PipelineContext context = pipelineService.createContext();
		context.put("start", "start");
		pipelineService.execute("interceptorTest", "base", context, response);
		Assert.assertEquals("starts1s2", response.getValue());
		Assert.assertEquals("s1s2",context.get("before"));
		Assert.assertEquals("s1s2",context.get("after"));

	}
}
