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

/**
 * @version $Revision: 83 $
 */
public enum PipelineFlowEnum implements PipelineFlow {
   STOP(true, false),
   STOP_AND_SUCCESS(true, true),
   CONTINUE(false, true);

   private boolean stopPipeline = false;
   private boolean success = true;

   PipelineFlowEnum(boolean stopPipeline, boolean success) {
      this.stopPipeline = stopPipeline;
      this.success = success;
   }

   public boolean isStopPipeline() {
      return stopPipeline;
   }

   public boolean isSuccess() {
      return success;
   }
}
