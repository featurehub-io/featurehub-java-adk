package io.featurehub.mr.sdk

import io.featurehub.admin.ApiClient
import io.featurehub.admin.api.ApplicationServiceApi
import io.featurehub.admin.api.FeatureServiceApi
import io.featurehub.admin.api.InfoServiceApi
import io.featurehub.admin.api.PortfolioServiceApi
import io.featurehub.admin.api.SetupServiceApi
import io.featurehub.admin.model.Application
import io.featurehub.admin.model.SetupSiteAdmin
import io.featurehub.admin.model.SortOrder
import spock.lang.Specification

import java.net.http.HttpRequest

class ClientIntegrationSpecs extends Specification {
  def "I can connect and list the existing features"() {
    given: "i have a connection"
      def api = new ApiClient()
      api.setHost("localhost").setPort(8085)
    and: "i attempt to get the version 30 times before giving up"
      def versionApi = new InfoServiceApi(api)
      def count = 0
      def found = false
      while (!found && count < 15) {
        try {
          versionApi.getInfoVersion()
          found = true
        } catch (Exception ignored) {
          ignored.printStackTrace()
          println("docker container not ready, sleeping for 2 seconds")
          Thread.sleep(2000)
          count++
        }
      }
      if (!found) {
        throw new RuntimeException("didn't come up in time")
      }
    and: "and i setup as the first user"

      def setup = new SetupServiceApi(api)
      def token = setup.setupSiteAdmin(
        new SetupSiteAdmin()
          .name("irina")
          .emailAddress("irina@i.com")
          .password("password123")
          .organizationName("Irina Inc")
          .portfolio("Test Portfolio")
      )

      // java 11 API isn't so nice with OpenAPI
      api.requestInterceptor = { HttpRequest.Builder request ->
        request.header("Authorization", "Bearer " + token.accessToken)
      }

    and: "i can get a list of portfolios i can access"
      def portfolioApi = new PortfolioServiceApi(api)
      def portfolios = portfolioApi.findPortfolios(false, true, SortOrder.ASC, null, null)
      println("portfolios are $portfolios")
    and: "i can create an application"
      def appApi = new ApplicationServiceApi(api)
      def app = appApi.createApplication(portfolios[0].id, new Application().name("app1").description("app1"), true, true)
    when: "i can get the list of features available in the first application in that portfolio"
      def featureApi = new FeatureServiceApi(api)
      def features = featureApi.findAllFeatureAndFeatureValuesForEnvironmentsByApplication(app.id)
//      println("features are $features")
    then:
      portfolios.size() == 1
      portfolios[0].name == 'Test Portfolio'
      features.features.size() == 0
  }
}
