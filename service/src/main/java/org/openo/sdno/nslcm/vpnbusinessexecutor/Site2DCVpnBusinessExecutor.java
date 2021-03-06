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

package org.openo.sdno.nslcm.vpnbusinessexecutor;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.openo.baseservice.remoteservice.exception.ServiceException;
import org.openo.sdno.nslcm.model.BusinessModel;
import org.openo.sdno.nslcm.model.Site2DCBusinessModel;
import org.openo.sdno.nslcm.serviceexecutor.OverlayVpnBusinessExecutor;
import org.openo.sdno.nslcm.serviceexecutor.ServiceChainBusinessExecutor;
import org.openo.sdno.nslcm.serviceexecutor.SiteBusinessExecutor;
import org.openo.sdno.nslcm.serviceexecutor.VpcBusinessExecutor;
import org.openo.sdno.nslcm.util.RecordProgress;
import org.openo.sdno.overlayvpn.model.servicechain.ServiceChainPath;
import org.openo.sdno.overlayvpn.model.servicemodel.Vpc;
import org.openo.sdno.overlayvpn.model.v2.overlay.NbiVpn;
import org.openo.sdno.overlayvpn.model.v2.site.NbiSiteModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Business executor of Site2DC OverlayVpn.<br>
 * 
 * @author
 * @version SDNO 0.5 2017-2-7
 */
@Component("Site2DCVpnBusinessExecutor")
public class Site2DCVpnBusinessExecutor implements VpnBusinessExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(Site2DCVpnBusinessExecutor.class);

    @Autowired
    private ServiceChainBusinessExecutor serviceChainBusinessExceutor;

    @Autowired
    private SiteBusinessExecutor siteBusinessExecutor;

    @Autowired
    private VpcBusinessExecutor vpcBusinessExecutor;

    @Autowired
    private OverlayVpnBusinessExecutor vpnBusinessExecutor;

    @Override
    public NbiVpn executeDeploy(BusinessModel businessModel) throws ServiceException {

        if(!(businessModel instanceof Site2DCBusinessModel)) {
            LOGGER.error("only Site2DCBusinessModel object can be processed here");
            throw new ServiceException("only Site2DCBusinessModel object can be processed here");
        }

        Site2DCBusinessModel site2DCBusinessModel = (Site2DCBusinessModel)businessModel;
        String instanceId = site2DCBusinessModel.getVpnModel().getId();

        if(null != site2DCBusinessModel.getSiteModel()) {
            siteBusinessExecutor.executeDeploy(instanceId, site2DCBusinessModel.getSiteModel());
        }

        if(null != site2DCBusinessModel.getVpcModel()) {
            vpcBusinessExecutor.executeDeploy(instanceId, site2DCBusinessModel.getVpcModel());
        }

        if(null != site2DCBusinessModel.getServiceChainPathModel()) {
            serviceChainBusinessExceutor.executeDeploy(site2DCBusinessModel.getServiceChainPathModel());
            RecordProgress.increaseCurrentStep(instanceId);
        }

        if(null != site2DCBusinessModel.getVpnModel()) {
            return vpnBusinessExecutor.executeDeploy(instanceId, site2DCBusinessModel.getVpnModel());
        }

        return null;
    }

    @Override
    public Map<String, String> executeUnDeploy(BusinessModel businessModel) throws ServiceException {

        if(!(businessModel instanceof Site2DCBusinessModel)) {
            LOGGER.error("only Site2DCBusinessModel object can be processed here");
            throw new ServiceException("only Site2DCBusinessModel object can be processed here");
        }

        Site2DCBusinessModel site2DCBusinessModel = (Site2DCBusinessModel)businessModel;

        String instanceId = site2DCBusinessModel.getVpnModel().getId();

        if(null != site2DCBusinessModel.getSiteModel()) {
            siteBusinessExecutor.executeUnDeploySubnet(instanceId, site2DCBusinessModel.getSiteModel());
        }

        if(null != site2DCBusinessModel.getVpnModel()) {
            vpnBusinessExecutor.executeUnDeploy(instanceId, site2DCBusinessModel.getVpnModel());
        }

        if(null != site2DCBusinessModel.getServiceChainPathModel()) {
            serviceChainBusinessExceutor.executeUnDeploy(site2DCBusinessModel.getServiceChainPathModel());
            RecordProgress.increaseCurrentStep(instanceId);
        }

        if(null != site2DCBusinessModel.getVpcModel()) {
            vpcBusinessExecutor.executeUnDeploy(instanceId, site2DCBusinessModel.getVpcModel());
        }

        if(null != site2DCBusinessModel.getSiteModel()) {
            siteBusinessExecutor.executeUnDeploy(instanceId, site2DCBusinessModel.getSiteModel());
        }

        Map<String, String> resultMap = new HashMap<>();
        resultMap.put("errorCode", businessModel.getVpnModel().getId());

        return resultMap;
    }

    @Override
    public Site2DCBusinessModel executeQuery(String vpnUuid) throws ServiceException {

        Site2DCBusinessModel businessModel = new Site2DCBusinessModel();

        NbiVpn vpn = vpnBusinessExecutor.executeQuery(vpnUuid);
        businessModel.setVpnModel(vpn);

        // ServiceChain has the same id with vpn, just fill in id field
        ServiceChainPath sfp = new ServiceChainPath();
        sfp.setUuid(vpn.getId());
        businessModel.setServiceChainPathModel(sfp);

        Set<String> siteUuidList = vpn.getSiteList();
        if(CollectionUtils.isEmpty(siteUuidList)) {
            LOGGER.error("No site related to vpn");
            throw new ServiceException("No site related to vpn");
        }

        String siteUuid = siteUuidList.iterator().next();
        NbiSiteModel siteModel = siteBusinessExecutor.executeQuery(siteUuid);
        businessModel.setSiteModel(siteModel);

        Set<String> vpcUuidList = vpn.getVpcList();
        if(CollectionUtils.isEmpty(vpcUuidList)) {
            LOGGER.error("No vpc related to vpn");
            throw new ServiceException("No vpc related to vpn");
        }

        String vpcUuid = vpcUuidList.iterator().next();
        Vpc vpcModel = vpcBusinessExecutor.executeQuery(vpcUuid);
        businessModel.setVpcModel(vpcModel);

        int total = 5;
        total += businessModel.getSiteModel().getLocalCpeModels().size()
                + businessModel.getSiteModel().getCloudCpeModels().size()
                + businessModel.getSiteModel().getVlans().size() + businessModel.getSiteModel().getSubnets().size()
                + businessModel.getVpcModel().getSubNetList().size()
                + businessModel.getVpnModel().getVpnGateways().size()
                + businessModel.getVpnModel().getVpnConnections().size();

        RecordProgress.setTotalSteps(vpnUuid, total);

        return businessModel;
    }

}
