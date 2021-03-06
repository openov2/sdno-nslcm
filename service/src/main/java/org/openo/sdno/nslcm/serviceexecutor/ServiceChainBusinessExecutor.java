/*
 * Copyright 2017 Huawei Technologies Co., Ltd.
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

package org.openo.sdno.nslcm.serviceexecutor;

import org.openo.baseservice.remoteservice.exception.ServiceException;
import org.openo.sdno.nslcm.sbi.servicechain.ServiceChainSbiService;
import org.openo.sdno.overlayvpn.model.servicechain.ServiceChainPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Executor class of ServiceChain Business.<br>
 * 
 * @author
 * @version SDNO 0.5 2017-2-7
 */
@Component
public class ServiceChainBusinessExecutor {

    @Autowired
    private ServiceChainSbiService serviceChainSbiService;

    /**
     * Deploy Service Chain.<br>
     * 
     * @param serviceChainPath ServiceChainPath need to deploy
     * @return ServiceChainPath deployed
     * @throws ServiceException when deploy failed
     * @since SDNO 0.5
     */
    public ServiceChainPath executeDeploy(ServiceChainPath serviceChainPath) throws ServiceException {
        serviceChainSbiService.createServiceChain(serviceChainPath);
        return serviceChainPath;
    }

    /**
     * UnDeploy Service Chain.<br>
     * 
     * @param serviceChainPath ServiceChainPath need to undeploy
     * @throws ServiceException when undeploy failed
     * @since SDNO 0.5
     */
    public void executeUnDeploy(ServiceChainPath serviceChainPath) throws ServiceException {
        serviceChainSbiService.deleteServiceChain(serviceChainPath.getUuid());
    }

    /**
     * Query ServiceChainPath.<br>
     * 
     * @param serviceChainUuid ServiceChainPath Uuid
     * @return ServiceChainPath queried out
     * @throws ServiceException when query failed
     * @since SDNO 0.5
     */
    public ServiceChainPath executeQuery(String serviceChainUuid) throws ServiceException {
        return serviceChainSbiService.queryServiceChain(serviceChainUuid);
    }

}
