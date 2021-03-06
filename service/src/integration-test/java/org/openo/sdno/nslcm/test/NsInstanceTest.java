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

package org.openo.sdno.nslcm.test;

import org.junit.Test;
import org.openo.baseservice.remoteservice.exception.ServiceException;
import org.openo.sdno.nslcm.responsechecker.StatusChecker;
import org.openo.sdno.testframework.http.model.HttpModelUtils;
import org.openo.sdno.testframework.http.model.HttpRquestResponse;
import org.openo.sdno.testframework.testmanager.TestManager;

public class NsInstanceTest extends TestManager {

    private static final String QUERY_UNEXIST_NSINSTANCE_TESTCASE =
            "src/integration-test/resources/testcase/queryunexistnsinstance.json";

    private static final String DELETE_UNEXIST_NSINSTANCE_TESTCASE =
            "src/integration-test/resources/testcase/deleteunexistnsinstance.json";

    @Test
    public void queryUnExistNsInstanceTest() throws ServiceException {
        HttpRquestResponse httpQueryNsInstanceObject =
                HttpModelUtils.praseHttpRquestResponseFromFile(QUERY_UNEXIST_NSINSTANCE_TESTCASE);
        execTestCase(httpQueryNsInstanceObject.getRequest(),
                new StatusChecker(httpQueryNsInstanceObject.getResponse()));
    }

    @Test
    public void deleteUnExistNsInstanceTest() throws ServiceException {
        HttpRquestResponse httpDeleteNsInstanceObject =
                HttpModelUtils.praseHttpRquestResponseFromFile(DELETE_UNEXIST_NSINSTANCE_TESTCASE);
        execTestCase(httpDeleteNsInstanceObject.getRequest(),
                new StatusChecker(httpDeleteNsInstanceObject.getResponse()));
    }

}
