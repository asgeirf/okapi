/*
 * Copyright 2007  T-Rank AS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.sf.okapi.common.pipeline;

import net.sf.okapi.common.resource.IResourceBuilder;

/**
 * The default exception handler. This will return a <tt>PipelineFlowEnum.STOP</tt> for all
 * exceptions.
 *
 * @version $Revision: 83 $
 *
 * @see no.trank.openpipe.api.PipelineFlow
 * @see no.trank.openpipe.api.PipelineFlowEnum
 */
public class DefaultPipelineExceptionHandler extends BasePipelineExceptionHandler {

   public PipelineFlow handlePrepareException(PipelineException ex) {
      notifyExceptionListeners(ex);
      return PipelineFlowEnum.STOP;
   }

   public void handleFinishException(PipelineException ex) {
      notifyExceptionListeners(ex);
   }

   public PipelineFlow handleProducerException(PipelineException ex) {
      notifyExceptionListeners(ex);
      return PipelineFlowEnum.STOP;
   }

   public PipelineFlow handleDocumentException(PipelineException ex, IResourceBuilder resourceBuilder) {
      notifyExceptionListeners(ex, resourceBuilder);
      return PipelineFlowEnum.STOP;
   }

}
