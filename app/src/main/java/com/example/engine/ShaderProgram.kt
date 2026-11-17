package com.example.engine

import android.opengl.GLES30
import android.util.Log

class LitShaderProgram {
    val programId: Int

    val uMvpMatrix: Int
    val uModelMatrix: Int
    val uColorTint: Int
    val uSunDir: Int
    val uSunColor: Int
    val uSkyAmbient: Int
    val uGroundAmbient: Int
    val uFlashPos: Int
    val uFlashColor: Int
    val uFlashIntensity: Int
    val uCameraPos: Int
    val uFogColor: Int
    val uFogDensity: Int
    val uDetailScale: Int
    val uContactAo: Int

    init {
        programId = linkProgram(VERTEX_SRC, FRAGMENT_SRC)
        uMvpMatrix = GLES30.glGetUniformLocation(programId, "uMvpMatrix")
        uModelMatrix = GLES30.glGetUniformLocation(programId, "uModelMatrix")
        uColorTint = GLES30.glGetUniformLocation(programId, "uColorTint")
        uSunDir = GLES30.glGetUniformLocation(programId, "uSunDir")
        uSunColor = GLES30.glGetUniformLocation(programId, "uSunColor")
        uSkyAmbient = GLES30.glGetUniformLocation(programId, "uSkyAmbient")
        uGroundAmbient = GLES30.glGetUniformLocation(programId, "uGroundAmbient")
        uFlashPos = GLES30.glGetUniformLocation(programId, "uFlashPos")
        uFlashColor = GLES30.glGetUniformLocation(programId, "uFlashColor")
        uFlashIntensity = GLES30.glGetUniformLocation(programId, "uFlashIntensity")
        uCameraPos = GLES30.glGetUniformLocation(programId, "uCameraPos")
        uFogColor = GLES30.glGetUniformLocation(programId, "uFogColor")
        uFogDensity = GLES30.glGetUniformLocation(programId, "uFogDensity")
        uDetailScale = GLES30.glGetUniformLocation(programId, "uDetailScale")
        uContactAo = GLES30.glGetUniformLocation(programId, "uContactAo")
    }

    fun use() = GLES30.glUseProgram(programId)

    fun release() = GLES30.glDeleteProgram(programId)

    companion object {
        private const val VERTEX_SRC = """#version 300 es
            layout(location = 0) in vec3 aPosition;
            layout(location = 1) in vec3 aNormal;
            layout(location = 2) in vec3 aColor;

            uniform mat4 uMvpMatrix;
            uniform mat4 uModelMatrix;

            out vec3 vWorldPos;
            out vec3 vNormal;
            out vec3 vColor;

            void main() {
                vec4 worldPos = uModelMatrix * vec4(aPosition, 1.0);
                vWorldPos = worldPos.xyz;
                vNormal = normalize(mat3(uModelMatrix) * aNormal);
                vColor = aColor;
                gl_Position = uMvpMatrix * vec4(aPosition, 1.0);
            }
        """

        private const val FRAGMENT_SRC = """#version 300 es
            precision highp float;

            in vec3 vWorldPos;
            in vec3 vNormal;
            in vec3 vColor;

            uniform vec3 uColorTint;
            uniform vec3 uSunDir;
            uniform vec3 uSunColor;
            uniform vec3 uSkyAmbient;
            uniform vec3 uGroundAmbient;
            uniform vec3 uFlashPos;
            uniform vec3 uFlashColor;
            uniform float uFlashIntensity;
            uniform vec3 uCameraPos;
            uniform vec3 uFogColor;
            uniform float uFogDensity;
            uniform float uDetailScale;
            uniform float uContactAo;

            out vec4 fragColor;

            float hash21(vec2 p) {
                p = fract(p * vec2(123.34, 456.21));
                p += dot(p, p + 45.32);
                return fract(p.x * p.y);
            }

            float valueNoise(vec2 p) {
                vec2 i = floor(p);
                vec2 f = fract(p);
                f = f * f * (3.0 - 2.0 * f);
                float a = hash21(i);
                float b = hash21(i + vec2(1.0, 0.0));
                float c = hash21(i + vec2(0.0, 1.0));
                float d = hash21(i + vec2(1.0, 1.0));
                return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
            }

            vec3 acesToneMap(vec3 x) {
                return clamp((x * (2.51 * x + 0.03)) / (x * (2.43 * x + 0.59) + 0.14), 0.0, 1.0);
            }

            void main() {
                vec3 N = normalize(vNormal);
                vec3 L = normalize(uSunDir);
                vec3 V = normalize(uCameraPos - vWorldPos);

                // Hemisphere ambient + subtle ground bounce
                float hemi = N.y * 0.5 + 0.5;
                vec3 ambient = mix(uGroundAmbient, uSkyAmbient, hemi);

                // Ground contact AO near floor plane
                float ao = 1.0;
                if (uContactAo > 0.5 && abs(N.y) < 0.7) {
                    ao = mix(0.62, 1.0, smoothstep(0.0, 0.9, vWorldPos.y));
                }

                // Sun diffuse + wrap fill + Blinn-Phong specular
                float nDotL = max(dot(N, L), 0.0);
                float wrapBack = max(dot(N, -L), 0.0) * 0.16;
                vec3 H = normalize(L + V);
                float specPower = (uDetailScale > 0.0) ? 28.0 : 42.0;
                float spec = pow(max(dot(N, H), 0.0), specPower) * 0.28 * nDotL;

                // Fresnel rim highlight for crisp silhouettes
                float fresnel = pow(1.0 - max(dot(N, V), 0.0), 3.2);
                vec3 rimLight = uSkyAmbient * fresnel * 0.34;

                // Dynamic point flash light (muzzle flash / explosion)
                vec3 toFlash = uFlashPos - vWorldPos;
                float flashDist = length(toFlash);
                float atten = uFlashIntensity / (1.0 + 0.06 * flashDist * flashDist);
                float flashDiff = max(dot(N, normalize(toFlash)), 0.0);
                vec3 flashLight = uFlashColor * flashDiff * atten;

                // Procedural surface micro-grain & architectural panel seams
                float detail = 1.0;
                if (uDetailScale > 0.0) {
                    vec2 uv = (abs(N.y) > 0.7) ? vWorldPos.xz : (abs(N.x) > 0.7 ? vWorldPos.zy : vWorldPos.xy);
                    vec2 grid = abs(fract(uv * uDetailScale) - 0.5);
                    float seam = smoothstep(0.475, 0.50, max(grid.x, grid.y));
                    float grain = valueNoise(uv * 2.4) * 0.14 - 0.07;
                    float strata = sin(vWorldPos.y * 5.2 + valueNoise(vWorldPos.xz * 0.6) * 2.0) * 0.03;
                    detail = 1.0 - seam * 0.16 + grain + strata;
                }

                vec3 albedo = vColor * uColorTint * detail;
                vec3 lit = albedo * (ambient * ao + uSunColor * (nDotL + wrapBack) + flashLight)
                         + uSunColor * spec
                         + rimLight;

                // Atmospheric distance fog with warm sun scattering
                float dist = length(uCameraPos - vWorldPos);
                float fogFactor = 1.0 - exp(-pow(dist * uFogDensity, 1.55));
                float sunScatter = pow(max(dot(-V, L), 0.0), 6.0);
                vec3 scatterFog = mix(uFogColor, uSunColor * 0.85, sunScatter * 0.38);

                vec3 combined = mix(lit, scatterFog, clamp(fogFactor, 0.0, 1.0));
                vec3 graded = acesToneMap(combined * 1.12);
                fragColor = vec4(graded, 1.0);
            }
        """
    }
}

class UnlitShaderProgram {
    val programId: Int
    val uMvpMatrix: Int
    val uColorAlpha: Int

    init {
        programId = linkProgram(VERTEX_SRC, FRAGMENT_SRC)
        uMvpMatrix = GLES30.glGetUniformLocation(programId, "uMvpMatrix")
        uColorAlpha = GLES30.glGetUniformLocation(programId, "uColorAlpha")
    }

    fun use() = GLES30.glUseProgram(programId)

    fun release() = GLES30.glDeleteProgram(programId)

    companion object {
        private const val VERTEX_SRC = """#version 300 es
            layout(location = 0) in vec3 aPosition;
            layout(location = 1) in vec3 aNormal;
            layout(location = 2) in vec3 aColor;

            uniform mat4 uMvpMatrix;
            out vec3 vColor;

            void main() {
                vColor = aColor;
                gl_Position = uMvpMatrix * vec4(aPosition, 1.0);
            }
        """

        private const val FRAGMENT_SRC = """#version 300 es
            precision mediump float;
            in vec3 vColor;
            uniform vec4 uColorAlpha;
            out vec4 fragColor;

            void main() {
                fragColor = vec4(vColor * uColorAlpha.rgb, uColorAlpha.a);
            }
        """
    }
}

private fun compileShader(type: Int, src: String): Int {
    val shader = GLES30.glCreateShader(type)
    GLES30.glShaderSource(shader, src)
    GLES30.glCompileShader(shader)
    val compiled = IntArray(1)
    GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compiled, 0)
    if (compiled[0] == 0) {
        val log = GLES30.glGetShaderInfoLog(shader)
        Log.e("IronfrontGL", "Shader compile error: $log")
        GLES30.glDeleteShader(shader)
        return 0
    }
    return shader
}

private fun linkProgram(vertSrc: String, fragSrc: String): Int {
    val vert = compileShader(GLES30.GL_VERTEX_SHADER, vertSrc)
    val frag = compileShader(GLES30.GL_FRAGMENT_SHADER, fragSrc)
    val program = GLES30.glCreateProgram()
    GLES30.glAttachShader(program, vert)
    GLES30.glAttachShader(program, frag)
    GLES30.glLinkProgram(program)
    GLES30.glDeleteShader(vert)
    GLES30.glDeleteShader(frag)
    return program
}
