package org.arquillian.spacelift.gradle

import org.apache.commons.lang3.SystemUtils



class ValueExtractor {

    def osMapping = [
        windows: { return SystemUtils.IS_OS_WINDOWS },
        mac: { return SystemUtils.IS_OS_MAC_OSX },
        linux: { return SystemUtils.IS_OS_LINUX },
        solaris: { return SystemUtils.IS_OS_SOLARIS || SystemUtils.IS_OS_SUN_OS },
    ]

    Closure extractValueAsLazyClosure(arg) {

        Closure retVal;
        if(arg instanceof Closure) {
            retVal = arg.dehydrate()
            retVal.resolveStrategy = Closure.DELEGATE_FIRST
            return retVal
        }
        else if(arg instanceof Map) {
            osMapping.each { platform, condition ->
                if(condition.call()) {
                    retVal = extractValueAsLazyClosure(arg[platform])
                    return
                }
            }
            if(retVal==null) {
                throw new IllegalStateException("Unknown system ${System.getProperty('os.name')}")
            }
            return retVal
        }

        // return value wrapper as closure
        return {arg}
    }

}
