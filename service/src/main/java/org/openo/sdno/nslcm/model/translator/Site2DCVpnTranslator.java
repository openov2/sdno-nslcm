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

package org.openo.sdno.nslcm.model.translator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.openo.baseservice.remoteservice.exception.ServiceException;
import org.openo.sdno.framework.container.util.JsonUtil;
import org.openo.sdno.framework.container.util.UuidUtils;
import org.openo.sdno.nslcm.config.OsDriverParamConfigReader;
import org.openo.sdno.nslcm.config.SiteParamConfigReader;
import org.openo.sdno.nslcm.dao.inf.IBaseResourceDao;
import org.openo.sdno.nslcm.model.BusinessModel;
import org.openo.sdno.nslcm.model.Site2DCBusinessModel;
import org.openo.sdno.nslcm.model.Site2DCTemplateModel;
import org.openo.sdno.nslcm.util.RecordProgress;
import org.openo.sdno.overlayvpn.brs.invdao.LogicalTernminationPointInvDao;
import org.openo.sdno.overlayvpn.brs.model.LogicalTernminationPointMO;
import org.openo.sdno.overlayvpn.brs.model.NetworkElementMO;
import org.openo.sdno.overlayvpn.brs.model.SiteMO;
import org.openo.sdno.overlayvpn.esr.invdao.VimInvDao;
import org.openo.sdno.overlayvpn.esr.model.Vim;
import org.openo.sdno.overlayvpn.model.servicechain.ServiceChainPath;
import org.openo.sdno.overlayvpn.model.servicechain.ServicePathHop;
import org.openo.sdno.overlayvpn.model.servicemodel.SubNet;
import org.openo.sdno.overlayvpn.model.servicemodel.Vpc;
import org.openo.sdno.overlayvpn.model.v2.cpe.CpeRoleType;
import org.openo.sdno.overlayvpn.model.v2.cpe.NbiCloudCpeModel;
import org.openo.sdno.overlayvpn.model.v2.cpe.NbiLocalCpeModel;
import org.openo.sdno.overlayvpn.model.v2.overlay.NbiVpn;
import org.openo.sdno.overlayvpn.model.v2.overlay.NbiVpnConnection;
import org.openo.sdno.overlayvpn.model.v2.overlay.NbiVpnGateway;
import org.openo.sdno.overlayvpn.model.v2.site.NbiSiteModel;
import org.openo.sdno.overlayvpn.model.v2.subnet.NbiSubnetModel;
import org.openo.sdno.overlayvpn.model.v2.vlan.NbiVlanModel;
import org.openo.sdno.overlayvpn.util.check.ValidationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Model translator class of Site2DC OverlayVpn.<br>
 * 
 * @author
 * @version SDNO 0.5 2017-1-25
 */
@Component("Site2DCVpnTranslator")
public class Site2DCVpnTranslator implements VpnTranslator {

    private static final Logger LOGGER = LoggerFactory.getLogger(Site2DCVpnTranslator.class);

    @Autowired
    private IBaseResourceDao baseResourceDao;

    @Autowired
    private SiteParamConfigReader siteParamConfigReader;

    @Autowired
    private OsDriverParamConfigReader osDriverParamConfigReader;

    @Autowired
    private VimInvDao vimInvDao;

    @Autowired
    private LogicalTernminationPointInvDao ltpInvDao;

    @Override
    public BusinessModel translateVpnModel(Map<String, Object> templateParameter, String instanceId)
            throws ServiceException {
        // Create Vpn template model and validate
        Site2DCTemplateModel vpnTemplateModel =
                JsonUtil.fromJson(JsonUtil.toJson(templateParameter), Site2DCTemplateModel.class);

        // Validate template model
        ValidationUtil.validateModel(vpnTemplateModel);

        // Translate Site2DC model
        return translateSite2DCVpnModel(vpnTemplateModel, instanceId);
    }

    private Site2DCBusinessModel translateSite2DCVpnModel(Site2DCTemplateModel templateModel, String instanceId)
            throws ServiceException {
        Site2DCBusinessModel businessModel = new Site2DCBusinessModel();

        businessModel.setSiteModel(translateSiteModel(templateModel));
        businessModel.setServiceChainPathModel(translateServiceChainPath(templateModel, instanceId));
        businessModel.setVpcModel(translateVpc(templateModel));
        businessModel.setVpnModel(translateVpnModel(templateModel, instanceId, businessModel.getVpcModel()));

        int total = 5;
        total += businessModel.getSiteModel().getLocalCpeModels().size()
                + businessModel.getSiteModel().getCloudCpeModels().size()
                + businessModel.getSiteModel().getVlans().size() + businessModel.getSiteModel().getSubnets().size()
                + businessModel.getVpcModel().getSubNetList().size()
                + businessModel.getVpnModel().getVpnGateways().size()
                + businessModel.getVpnModel().getVpnConnections().size();

        RecordProgress.setTotalSteps(instanceId, total);

        return businessModel;
    }

    private NbiSiteModel translateSiteModel(Site2DCTemplateModel templateModel) throws ServiceException {

        SiteMO brsSiteMO = baseResourceDao.querySiteByName(templateModel.getSiteName());
        NetworkElementMO localCpeNe = baseResourceDao.querySiteCpeByType(brsSiteMO.getId(), CpeRoleType.THIN_CPE);
        NetworkElementMO cloudCpeNe = baseResourceDao.querySiteCpeByType(brsSiteMO.getId(), CpeRoleType.CLOUD_CPE);

        // Create SiteModel
        NbiSiteModel siteModel = new NbiSiteModel();
        siteModel.setUuid(brsSiteMO.getId());
        siteModel.initBasicInfo(brsSiteMO.getName(), brsSiteMO.getTenantID(), null, brsSiteMO.getDescription());
        siteModel.setLocalCpeType(localCpeNe.getProductName());
        siteModel.setReliability(siteParamConfigReader.getSiteReliability());
        siteModel.setSiteDescriptor(templateModel.getVpnType());
        siteModel.setIsEncrypt(siteParamConfigReader.getSiteIsEncrypt());

        // Create LocalCpe
        NbiLocalCpeModel localCpeModel = new NbiLocalCpeModel();
        localCpeModel.setUuid(localCpeNe.getId());
        localCpeModel.setName(localCpeNe.getName());
        localCpeModel.setTenantId(brsSiteMO.getTenantID());
        localCpeModel.setDescription(localCpeNe.getDescription());
        localCpeModel.setSiteId(brsSiteMO.getId());
        localCpeModel.setEsn(localCpeNe.getSerialNumber());
        localCpeModel.setControllerId(localCpeNe.getControllerID().get(0));
        localCpeModel.setLocalCpeType(localCpeNe.getProductName());

        siteModel.setLocalCpeModels(Arrays.asList(localCpeModel));

        // Create CloudCpe
        NbiCloudCpeModel cloudCpeModel = new NbiCloudCpeModel();
        cloudCpeModel.setUuid(cloudCpeNe.getId());
        cloudCpeModel.setName(cloudCpeNe.getName());
        cloudCpeModel.setTenantId(brsSiteMO.getTenantID());
        cloudCpeModel.setDescription(cloudCpeNe.getDescription());
        cloudCpeModel.setSiteId(brsSiteMO.getId());
        cloudCpeModel.setEsn(cloudCpeNe.getSerialNumber());
        cloudCpeModel.setControllerId(cloudCpeNe.getControllerID().get(0));

        siteModel.setCloudCpeModels(Arrays.asList(cloudCpeModel));

        // Create VlanModel
        NbiVlanModel vlanModel = new NbiVlanModel();
        vlanModel.setUuid(UuidUtils.createUuid());
        vlanModel.setName("Vlan_" + templateModel.getVpnName());
        vlanModel.setTenantId(brsSiteMO.getTenantID());
        vlanModel.setDescription(templateModel.getVpnDescription());
        vlanModel.setSiteId(brsSiteMO.getId());
        vlanModel.setVlanId(templateModel.getSubnetVlan());

        siteModel.setVlans(Arrays.asList(vlanModel));

        // Create SubnetModel
        NbiSubnetModel subnetModel = new NbiSubnetModel();
        subnetModel.setUuid(UuidUtils.createUuid());
        subnetModel.setName("Subnet_" + templateModel.getVpnName());
        subnetModel.setTenantId(brsSiteMO.getTenantID());
        subnetModel.setDescription(templateModel.getVpnDescription());
        subnetModel.setSiteId(brsSiteMO.getId());
        subnetModel.setCidrBlock(templateModel.getSiteCidr());
        subnetModel.setVni(templateModel.getSiteVni().toString());

        siteModel.setSubnets(Arrays.asList(subnetModel));

        return siteModel;
    }

    private NbiVpn translateVpnModel(Site2DCTemplateModel templateModel, String instanceId, Vpc vpcModel)
            throws ServiceException {

        SiteMO brsSiteMO = baseResourceDao.querySiteByName(templateModel.getSiteName());

        // Create Vpn
        NbiVpn vpn = new NbiVpn();
        vpn.setId(instanceId);
        vpn.setName(templateModel.getVpnName());
        vpn.setTenantId(brsSiteMO.getTenantID());
        vpn.setDescription(templateModel.getVpnDescription());
        vpn.setVpnDescriptor(templateModel.getVpnType());

        // Create Site Gateway
        NbiVpnGateway siteGateway = new NbiVpnGateway();
        siteGateway.setId(UuidUtils.createUuid());
        siteGateway.setName("SiteGateway_" + templateModel.getVpnName());
        siteGateway.setTenantId(brsSiteMO.getTenantID());
        siteGateway.setDescription(templateModel.getVpnDescription());
        siteGateway.setSiteId(brsSiteMO.getId());
        siteGateway.setVpnId(vpn.getId());
        siteGateway.setPorts(queryPorts(templateModel).get("portId"));
        siteGateway.setPortNames(queryPorts(templateModel).get("portName"));

        vpn.setVpnGateways(new ArrayList<NbiVpnGateway>());
        vpn.getVpnGateways().add(siteGateway);

        // Create Vpc Gateway
        NbiVpnGateway vpcGateway = new NbiVpnGateway();
        vpcGateway.setId(UuidUtils.createUuid());
        vpcGateway.setName("VpcGateway_" + templateModel.getVpnName());
        vpcGateway.setTenantId(brsSiteMO.getTenantID());
        vpcGateway.setDescription(templateModel.getVpnDescription());
        vpcGateway.setVpcId(vpcModel.getUuid());
        vpcGateway.setVpnId(vpn.getId());
        vpcGateway.setSubnets(new ArrayList<String>());
        vpcGateway.getSubnets().add(vpcModel.getSubNetList().get(0).getUuid());

        vpn.getVpnGateways().add(vpcGateway);

        // Create Vpn Connection
        NbiVpnConnection vpnConnection = new NbiVpnConnection();
        vpnConnection.setId(UuidUtils.createUuid());
        vpnConnection.setName("VpnConnection_" + templateModel.getVpnName());
        vpnConnection.setTenantId(brsSiteMO.getTenantID());
        vpnConnection.setDescription(templateModel.getVpnDescription());
        vpnConnection.setaEndVpnGatewayId(siteGateway.getId());
        vpnConnection.setzEndVpnGatewayId(vpcGateway.getId());
        vpnConnection.setVpnId(vpn.getId());
        vpnConnection.setVni(templateModel.getSiteVni().toString());
        vpnConnection.setDeployStatus("deploy");

        vpn.setVpnConnections(Arrays.asList(vpnConnection));

        return vpn;
    }

    private ServiceChainPath translateServiceChainPath(Site2DCTemplateModel templateModel, String instanceId)
            throws ServiceException {

        String dcGwIp = templateModel.getDcGwIp();
        String dcFwIp = templateModel.getDcFwIp();
        String dcLbIp = templateModel.getDcLbIp();

        if(StringUtils.isEmpty(dcGwIp) || StringUtils.isEmpty(dcFwIp) || StringUtils.isEmpty(dcLbIp)) {
            LOGGER.info("ServiceChain Ne IpAddress is empty, Service Chain will not be created.");
            return null;
        }

        ServiceChainPath sfpPath = new ServiceChainPath();
        sfpPath.setUuid(instanceId);
        sfpPath.setName("Sfp_" + templateModel.getVpnName());
        sfpPath.setDescription(templateModel.getVpnDescription());
        NetworkElementMO gwNe = baseResourceDao.queryNeByIpAddress(dcGwIp);
        sfpPath.setScfNeId(gwNe.getId());

        NetworkElementMO fwNe = baseResourceDao.queryNeByIpAddress(dcFwIp);
        ServicePathHop fwPathHop = new ServicePathHop();
        fwPathHop.setHopNumber(1);
        fwPathHop.setSfiId(fwNe.getId());
        sfpPath.setServicePathHops(new ArrayList<ServicePathHop>());
        sfpPath.getServicePathHops().add(fwPathHop);

        NetworkElementMO lbNe = baseResourceDao.queryNeByIpAddress(dcLbIp);
        ServicePathHop lbPathHop = new ServicePathHop();
        lbPathHop.setHopNumber(2);
        lbPathHop.setSfiId(lbNe.getId());
        sfpPath.getServicePathHops().add(lbPathHop);

        return sfpPath;
    }

    private Vpc translateVpc(Site2DCTemplateModel templateModel) throws ServiceException {

        Vpc vpc = new Vpc();

        vpc.setUuid(UuidUtils.createUuid());
        vpc.setName(templateModel.getVpcName());
        vpc.setDescription(templateModel.getVpnDescription());
        vpc.setOsControllerId(queryOsControllerId(osDriverParamConfigReader.getVimName()));
        SubNet subNet = vpc.getSubNetList().get(0);
        subNet.setUuid(UuidUtils.createUuid());
        subNet.setName(templateModel.getVpcSubnetName());
        subNet.setVpcId(vpc.getUuid());
        subNet.setCidr(templateModel.getVpcSubnetCidr());
        subNet.setVni(templateModel.getVpcVni());

        return vpc;
    }

    private String queryOsControllerId(String openStackName) throws ServiceException {

        Vim osVim = vimInvDao.queryVimByName(openStackName);
        if(null == osVim) {
            LOGGER.error("This openstack controller does not exist");
            throw new ServiceException("This openstack controller does not exist");
        }

        return osVim.getVimId();
    }

    private Map<String, List<String>> queryPorts(Site2DCTemplateModel templateModel) throws ServiceException {
        SiteMO brsSiteMO = baseResourceDao.querySiteByName(templateModel.getSiteName());
        NetworkElementMO localCpeNe = baseResourceDao.querySiteCpeByType(brsSiteMO.getId(), CpeRoleType.THIN_CPE);
        Map<String, List<String>> portInfo = new HashMap<>();

        List<String> ltpIdList = new ArrayList<>();
        List<String> ltpNameList = new ArrayList<>();

        Map<String, String> condition = new HashMap<String, String>();
        condition.put("meID", localCpeNe.getId());
        List<LogicalTernminationPointMO> localLtpMOList = ltpInvDao.query(condition);
        for(LogicalTernminationPointMO ltpMo : localLtpMOList) {
            ltpIdList.add(ltpMo.getId());
            ltpNameList.add(ltpMo.getName());
        }

        portInfo.put("portId", ltpIdList);
        portInfo.put("portName", ltpNameList);

        return portInfo;
    }

}
