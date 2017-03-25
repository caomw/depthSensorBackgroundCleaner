#version 300 es

precision mediump float;
precision mediump isampler2D;

in vec2 v_texCoord;

layout(location = 0) out vec4 outColor;

uniform sampler2D s_backgroundMap; 
uniform sampler2D s_rgbMap;
uniform sampler2D s_depthMap;
uniform int u_isTracked;

void main()           
{
	vec4 back= texture( s_backgroundMap, v_texCoord ); 
	vec4 fore = texture( s_rgbMap, v_texCoord); 
  	float alpha = 0.9f * textureLod(s_depthMap, v_texCoord, 1.1f).r;
 	alpha += 0.5f * textureLod(s_depthMap, v_texCoord, 2.2f).r;
 	alpha += 0.3f * textureLod(s_depthMap, v_texCoord, 3.3f).r;
 	//alpha += 0.2f * textureLod(s_depthMap, v_texCoord, 4.4f).r;
 	
 	alpha = min(alpha, 1.0f);
  	
	if (u_isTracked == 0)
	{
		alpha = 0.5f;
	}

	if (true)
	{
		outColor = mix(fore, back, 1.0f - alpha);
	}
  	else
  	{
  		outColor  =vec4(alpha, alpha, alpha,0);
  	}
}