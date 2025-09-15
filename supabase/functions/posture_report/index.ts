// // deno-lint-ignore-file no-explicit-any
// /// <reference lib="deno.unstable" />
// import "jsr:@supabase/functions-js/edge-runtime.d.ts";
// import { createClient } from "jsr:@supabase/supabase-js@2";

// // Env
// const OPENAI_API_KEY = Deno.env.get("OPENAI_API_KEY")!;
// const OPENAI_MODEL = Deno.env.get("OPENAI_MODEL") ?? "gpt-4o";

// // Prompt
// const PROMPT = `You are a posture analyst.
// Use the FRONT and SIDE skeleton-overlay images.
// Identify regions with issues and select posture types ONLY from:
// [FWD_HEAD, CERVICAL_FLEXION_LIMIT, HYPERKYPHOSIS_THRX, HYPERLORDOSIS_LUMBAR, FLAT_BACK, SWAY_BACK, ANTERIOR_PELVIC_TILT, POSTERIOR_PELVIC_TILT, HEAD_LATERAL_TILT, SCAPULAR_WINGING, ROUNDED_SHOULDERS, SHOULDER_ELEVATION_ASYM, PELVIC_OBLIQUITY, FUNCTIONAL_SCOLIOSIS, LOWER_LIMB_ALIGNMENT]
// Return ONLY valid JSON:

// {
//   "version": "posture-issues.v1",
//   "overall": { "score": number, "risk": "low"|"medium"|"high" },
//   "regions": Array<{ "region": "head"|"neck"|"shoulders"|"thoracic"|"lumbar"|"pelvis"|"knees"|"feet",
//                       "status": "ok"|"issue"|"severe", "notes": string }>,
//   "posture_types": Array<{
//     "id": string, "name": string, "plane": "sagittal"|"frontal",
//     "severity": "mild"|"moderate"|"severe", "confidence": number,
//     "affected_regions": string[], "metrics"?: object
//   }>,
//   "counts": { "issues_detected": number, "types_detected": number }
// }`;

// function asImageUrl(frontBase64?: string, sideBase64?: string, frontUrl?: string, sideUrl?: string) {
//   const f = frontBase64
//     ? `data:image/jpeg;base64,${frontBase64}`
//     : (frontUrl ?? "");
//   const s = sideBase64
//     ? `data:image/jpeg;base64,${sideBase64}`
//     : (sideUrl ?? "");
//   return { f, s };
// }

// export default Deno.serve(async (req) => {
//   try {
//     const supabase = createClient(
//       Deno.env.get("SUPABASE_URL")!,
//       Deno.env.get("SUPABASE_ANON_KEY")!,
//       { global: { headers: { Authorization: req.headers.get("Authorization") ?? "" } } }
//     );

//     const { data: userRes, error: authErr } = await supabase.auth.getUser();
//     if (authErr || !userRes?.user) {
//       return new Response(JSON.stringify({ error: "auth" }), { status: 401 });
//     }

//     const body = await req.json();
//     const scanId: string = body.scanId ?? crypto.randomUUID();
//     const frontBase64: string | undefined = body.frontBase64;
//     const sideBase64: string | undefined = body.sideBase64;
//     const frontUrl: string | undefined = body.frontUrl;
//     const sideUrl: string | undefined = body.sideUrl;

//     const { f: frontImageUrl, s: sideImageUrl } = asImageUrl(
//       frontBase64,
//       sideBase64,
//       frontUrl,
//       sideUrl,
//     );

//     if (!frontImageUrl || !sideImageUrl) {
//       return new Response(JSON.stringify({ error: "missing_images" }), { status: 400 });
//     }

//     // OpenAI Responses call (multimodal)
//     const r = await fetch("https://api.openai.com/v1/responses", {
//       method: "POST",
//       headers: {
//         "Authorization": `Bearer ${OPENAI_API_KEY}`,
//         "Content-Type": "application/json",
//       },
//       body: JSON.stringify({
//         model: OPENAI_MODEL,
//         input: [
//           {
//             role: "user",
//             content: [
//               { type: "input_text", text: PROMPT },
//               { type: "input_image", image_url: frontImageUrl },
//               { type: "input_image", image_url: sideImageUrl },
//             ],
//           },
//         ],
//       }),
//     });

//     if (!r.ok) {
//       const errTxt = await r.text();
//       return new Response(JSON.stringify({ error: "openai_error", detail: errTxt }), { status: 502 });
//     }

//     const oai = await r.json() as any;
//     let text = oai.output_text ?? "";
//     if (!text && Array.isArray(oai.output)) {
//       for (const item of oai.output) {
//         if (Array.isArray(item.content)) {
//           const t = item.content.find((c: any) => c.text)?.text;
//           if (t) { text = t; break; }
//         }
//       }
//     }
//     if (!text) {
//       return new Response(JSON.stringify({ error: "no_text" }), { status: 500 });
//     }

//     let report: any;
//     try { report = JSON.parse(text); }
//     catch {
//       return new Response(JSON.stringify({ error: "invalid_json", raw: text }), { status: 500 });
//     }

//     const { error: ierr, data } = await supabase
//       .from("posture_reports")
//       .insert({
//         user_id: userRes.user.id,
//         scan_id: scanId,
//         model: OPENAI_MODEL,
//         views: ["front", "side"],
//         report,
//       })
//       .select()
//       .single();

//     if (ierr) {
//       return new Response(JSON.stringify({ error: "db", detail: ierr.message }), { status: 500 });
//     }

//     return new Response(JSON.stringify({ ok: true, id: data.id, report: data.report }), {
//       headers: { "Content-Type": "application/json" },
//     });
//   } catch (e) {
//     return new Response(JSON.stringify({ error: "server", detail: String(e) }), { status: 500 });
//   }
// });


