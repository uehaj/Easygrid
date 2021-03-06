package org.grails.plugin.easygrid

import groovy.util.logging.Slf4j
import org.codehaus.groovy.grails.plugins.web.taglib.ValidationTagLib
import org.springframework.core.NamedThreadLocal
import org.springframework.web.context.request.RequestContextHolder

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpSession
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap

/**
 * utility class
 * - stores the current GridConfig to ThreadLocal
 * - is used by services to access http parameters, session, etc
 * - stores the parameters
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
@Slf4j
class EasygridContextHolder {

    // used to store the gridConfig between method calls
    private static final ThreadLocal<GridConfig> gridConfigHolder = new NamedThreadLocal<GridConfig>("gridConfigHolder")

    // in case we need to work with an old set of parameters ( ex: exporting data already filtered , or returning from an add/update page )
    private static final ThreadLocal restoredParamsHolder = new NamedThreadLocal("restoredParamsHolder")

    static GridConfig getGridConfig() {
        gridConfigHolder.get()
    }

    static setLocalGridConfig(GridConfig config) {
        gridConfigHolder.set(config)
    }

    static storeParams(params) {
        restoredParamsHolder.set(params)
    }

    static resetParams() {
        restoredParamsHolder.remove()
    }

    static GrailsParameterMap getParams() {
        def params = restoredParamsHolder.get()
        params ? params : RequestContextHolder.currentRequestAttributes().params
    }

    static HttpServletRequest getRequest() {
        RequestContextHolder.currentRequestAttributes().request
    }

    static HttpServletResponse getResponse() {
        RequestContextHolder.currentRequestAttributes().currentResponse
    }

    static HttpSession getSession() {
        RequestContextHolder.currentRequestAttributes().session
    }

    static getFlashScope() {
        RequestContextHolder.currentRequestAttributes().flashScope
    }

    /**
     * utility class for i18n
     * @param code
     * @return
     */
    static messageLabel(code) {
        new ValidationTagLib().message(code: code)
    }


    // used for development - reload grids when modifying controllers or services
    private static reloadGrids = true
    private static final ReadWriteLock reloadLock = new ReentrantReadWriteLock();
    private static final Lock readLock = reloadLock.readLock();
    private static final Lock writeLock = reloadLock.writeLock();

    def reloadGrids() {
        readLock.lock()
        try {
            def old = reloadGrids
            reloadGrids = false
            old
        } finally {
            readLock.unlock()
        }
    }

    /**
     * announce the grids that a reload is necessary
     * @return
     */
    static classReloaded() {
        writeLock.lock()
        try {
            log.debug 'reload grids'
            reloadGrids = true
        } finally {
            writeLock.unlock()
        }
    }
}
