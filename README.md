<div align="center">
  <img alt="Logo" src="./PluginsAndFeatures/azure-toolkit-for-rider/src/main/resources/META-INF/pluginIcon.svg#gh-light-mode-only" width="100">
  <img alt="Logo" src="./PluginsAndFeatures/azure-toolkit-for-rider/src/main/resources/META-INF/pluginIcon_dark.svg#gh-dark-mode-only" width="100">
  <h2>Azure Toolkit for Rider</h2>

The Azure Toolkit for JetBrains Rider is an open-source project that helps .NET
developers easily create, develop, configure, test, manage, and deploy highly available and scalable web apps to Azure.

  <a href="https://github.com/JetBrains"><img src="http://jb.gg/badges/official-flat-square.svg" alt="JetBrains Official"></a>
  <a href="https://plugins.jetbrains.com/plugin/11220-azure-toolkit-for-rider"><img src="https://img.shields.io/jetbrains/plugin/v/11220-azure-toolkit-for-rider.svg?label=plugin&logo=rider" alt="Version"></a>
  <a href="https://plugins.jetbrains.com/plugin/11220-azure-toolkit-for-rider"><img src="https://img.shields.io/jetbrains/plugin/d/11220-azure-toolkit-for-rider.svg" alt="Downloads"></a>
</div>

---

The plugin can be downloaded and installed in JetBrains Rider and is
available [from the JetBrains plugins repository](https://plugins.jetbrains.com/plugin/11220-azure-toolkit-for-rider).

## Functionality

* Sign in to an Azure account
* Manage one or multiple subscriptions
* Basic management of container hosts (create/delete, start/stop, view details)
* List container registries
* Basic management of Redis caches (create/delete, start/stop, view details, list keys/values)
* Basic management of Sql databases (list, create, delete, open in browser, add firewall rule for current public IP,
  connect to a database in database tools)
* Basic management of storage accounts (create/delete, list/create/delete blob container, list/upload/download/delete
  blobs, connect to emulator)
* Basic management of virtual machines (create/delete, start/stop, view details)
* Basic management of web apps and deployment slots (create/delete, start/stop, view details, edit settings, swap slot)
* Basic management of function apps and deployment slots (create/delete, start/stop, view details, edit settings, swap
  slot)
* Azure Resource Manager (list/expand resource groups, deploy ARM template, import/export parameter file, edit
  deployment)
* Run configuration to deploy function apps and ASP.NET web apps (can also provision SQL database)
    * ASP.NET Core web apps (any platform)
    * .NET framework web apps (Windows)
* Azure Functions
    * Azure Functions Core Tools integration with project and item templates
    * Run configuration to run/debug .NET Core function apps
    * Run function from gutter
* Azure Cloud Shell support
    * Connect to cloud shell and work with terminal
    * Upload file action in Rider, support `download <file>` command in terminal
    * Support for `az aks browse` command opening browser
    * Support for web app preview ports

Feature requests can be logged in our [issue tracker](https://github.com/JetBrains/azure-tools-for-intellij/issues), we
also welcome contributions.

## Resources

* [Issue tracker](https://github.com/JetBrains/azure-tools-for-intellij/issues)
* [Plugin page](https://plugins.jetbrains.com/plugin/11220-azure-toolkit-for-rider)

## History and differences with Microsoft Azure Toolkit for IntelliJ

The Azure Toolkit for [JetBrains Rider](https://www.jetbrains.com/rider) is a fork of
the [Azure Toolkit for IntelliJ](https://docs.microsoft.com/en-us/java/azure/intellij/azure-toolkit-for-intellij-installation),
available [on GitHub](https://github.com/Microsoft/azure-tools-for-java).

Microsoft's Azure Toolkit for IntelliJ provides similar functionality to the Azure plugin
for [JetBrains Rider](https://www.jetbrains.com/rider), however, focus on the Java/JVM ecosystem and development flows.
JetBrains decided to fork the original plugin, and split base functionality (such as browsing Azure resources) from
Java/JVM-specific features (such as deploying a `.war` file to the HDInsight service).

The Azure Toolkit for [JetBrains Rider](https://www.jetbrains.com/rider) is released with several notable differences:

* No telemetry or usage data is collected and sent to Microsoft
* Java/JVM-specific functionality was removed
* .NET-specific functionalities, such as deploying an ASP.NET web application and more, have been added

## Contributing

Please see the [contribution instructions](CONTRIBUTING.md) if you wish to build the plugin from source.