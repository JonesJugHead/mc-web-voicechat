
export async function fetchAuthRequiredStatus(): Promise<{ authRequired: boolean }> {
    const response = await fetch('/authstatus');
    const data = await response.json();
    return data;
}