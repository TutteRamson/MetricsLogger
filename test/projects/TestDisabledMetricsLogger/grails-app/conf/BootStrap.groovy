import org.apache.log4j.BasicConfigurator

class BootStrap {

	def init = { servletContext ->
		BasicConfigurator.configure()
	}
	def destroy = {
	}
}
