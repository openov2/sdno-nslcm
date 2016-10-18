/*
 * Copyright 2016 Huawei Technologies Co., Ltd.
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

package org.openo.sdno.nslcm.util.exception;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Application exception.<br>
 * <p>
 * </p>
 * 
 * @author
 * @version SDNO 0.5 Oct 18, 2016
 */
public class ApplicationException extends WebApplicationException {

    /**
     * Serial number.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Constructor<br>
     * <p>
     * </p>
     * 
     * @param errorCode error status
     * @param errorDetail error detail
     * @since SDNO 0.5
     */
    public ApplicationException(int errorCode, Object errorDetail) {
        super(Response.status(errorCode).entity(errorDetail).type(MediaType.APPLICATION_JSON).build());
    }
}
