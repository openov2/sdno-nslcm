/*
 * Copyright 2016-2017 Huawei Technologies Co., Ltd.
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

package org.openo.sdno.nslcm.sbi.catalog;

import java.text.MessageFormat;
import java.util.Map;

import org.codehaus.jackson.type.TypeReference;
import org.openo.baseservice.remoteservice.exception.ServiceException;
import org.openo.baseservice.roa.util.restclient.RestfulResponse;
import org.openo.sdno.framework.container.resthelper.RestfulProxy;
import org.openo.sdno.framework.container.util.JsonUtil;
import org.openo.sdno.nslcm.util.AdapterUrlConst;
import org.openo.sdno.nslcm.util.RestfulParametersUtil;
import org.openo.sdno.rest.ResponseUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * SBI service of Catalog.<br>
 * 
 * @author
 * @version SDNO 0.5 June 22, 2016
 */
@Service
public class CatalogSbiService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CatalogSbiService.class);

    /**
     * Query template information from catalog. <br>
     * 
     * @param nsdId ID of the template in catalog used to create the SDN-O service instance
     * @return The template information
     * @throws ServiceException When query failed
     * @since SDNO 0.5
     */
    public Map<String, Object> queryServiceTemplate(String nsdId) throws ServiceException {
        String url = MessageFormat.format(AdapterUrlConst.CATALOG_ADAPTER_URL + "/{0}", nsdId);

        LOGGER.info("queryServiceTemplate begin: " + url);

        RestfulResponse response = RestfulProxy.get(url, RestfulParametersUtil.getRestfulParameters());
        String rspContent = ResponseUtils.transferResponse(response);
        Map<String, Object> restResult = JsonUtil.fromJson(rspContent, new TypeReference<Map<String, Object>>() {});

        LOGGER.info("queryServiceTemplate end, result = " + restResult.toString());

        return restResult;
    }
}